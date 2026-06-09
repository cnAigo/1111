package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestConfigEntity;
import org.example.testvue.entity.TestHistory;
import org.example.testvue.repository.TestConfigRepository;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates test execution and environment cleanup.
 * All state is persisted to DB via TestHistory — no volatile fields, no memory-backed log buffer.
 * Threads are managed by the configured ThreadPoolTaskExecutor, not raw new Thread().
 */
@Service
public class TestExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TestExecutionService.class);

    private final TestHistoryRepository historyRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final TestConfigRepository configRepo;
    private final MavenTestRunner mavenRunner;
    private final CleanupService cleanupService;

    /** Shared stop flag for the currently running task */
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    public TestExecutionService(TestHistoryRepository historyRepo,
                                TestCaseDetailRepository caseDetailRepo,
                                TestConfigRepository configRepo,
                                MavenTestRunner mavenRunner,
                                CleanupService cleanupService) {
        this.historyRepo = historyRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.configRepo = configRepo;
        this.mavenRunner = mavenRunner;
        this.cleanupService = cleanupService;
    }

    // ── Status helpers (read from DB) ──

    private TestHistory getCurrent() {
        return historyRepo.findTopByOrderByCreateTimeDesc();
    }

    public boolean isRunning() {
        TestHistory h = historyRepo.findByTaskId(getCurrentTaskId());
        return h != null && "RUNNING".equals(h.getStatus());
    }

    private String getCurrentTaskId() {
        TestHistory h = getCurrent();
        return h != null ? h.getTaskId() : null;
    }

    // ── Get current status (reads from DB + disk log) ──

    public StatusResponse getCurrentStatus() {
        StatusResponse s = new StatusResponse();
        TestHistory h = getCurrent();
        if (h != null) {
            s.status = h.getStatus() != null ? h.getStatus() : "IDLE";
            s.msg = h.getErrorMessage() != null ? h.getErrorMessage() : "";
            s.label = h.getLabel() != null ? h.getLabel() : "";
            s.durationMs = h.getDurationMs();
            s.durationFmt = h.getDurationFmt() != null ? h.getDurationFmt() : "";
            s.progress = h.getProgress();
            s.progressTotal = h.getProgressTotal();
            s.errorMessage = h.getErrorMessage() != null ? h.getErrorMessage() : "";
            s.taskId = h.getTaskId();
            // Read log from disk file (not from memory buffer)
            s.output = MavenTestRunner.readLog(h.getTaskId());
        } else {
            s.status = "IDLE"; s.label = ""; s.durationFmt = "";
        }
        return s;
    }

    // ── Run ──

    public synchronized String startRun(TestRunRequest req) {
        if (isRunning()) return null;
        stopFlag.set(false);

        String tid = UUID.randomUUID().toString();
        String lb = resolveLabel(req);

        int total = mavenRunner.countTestCases(req);
        if (total <= 0) total = 1;

        // Persist initial record immediately
        TestHistory h = TestHistory.of(tid, lb, "RUNNING", "", 0, 0, 0, "", "");
        h.setProgress(0);
        h.setProgressTotal(total);
        historyRepo.save(h);

        // Execute asynchronously via thread pool
        executeRunAsync(tid, req, lb);
        return tid;
    }

    @Async("testExecutor")
    public void executeRunAsync(String tid, TestRunRequest req, String label) {
        try {
            mavenRunner.run(tid, req, label, stopFlag);
        } catch (Exception e) {
            LOG.error("Test execution failed", e);
            TestHistory h = historyRepo.findByTaskId(tid);
            if (h != null) {
                h.setStatus("FAILED");
                h.setErrorMessage(e.getMessage());
                h.setOutput(e.getMessage());
                historyRepo.save(h);
            }
        }
    }

    public void stopRun() {
        stopFlag.set(true);
        mavenRunner.kill();
        cleanupService.stop();

        // Update DB
        TestHistory h = historyRepo.findByTaskId(getCurrentTaskId());
        if (h != null && "RUNNING".equals(h.getStatus())) {
            h.setStatus("IDLE");
            h.setErrorMessage("手动停止");
            historyRepo.save(h);
        }
    }

    // ── Cleanup ──

    public synchronized void startCleanup(String projectId, String url,
                                           String username, String password) {
        if (isRunning()) return;
        stopFlag.set(false);

        String pId = resolveProjectId(projectId);
        String base = (url != null && !url.isBlank()) ? url : "https://192.168.6.171:8088";
        String user = (username != null && !username.isBlank()) ? username : "admin";
        String pass = (password != null && !password.isBlank()) ? password : "Aa123456";

        String tid = "cleanup-" + System.currentTimeMillis();
        TestHistory h = TestHistory.of(tid, "清理环境", "RUNNING", "", 0, 0, 0, "", "");
        h.setProgress(0); h.setProgressTotal(0);
        historyRepo.save(h);

        runCleanupAsync(base, user, pass, pId, tid);
    }

    @Async("testExecutor")
    public void runCleanupAsync(String base, String user, String pass, String projectId, String tid) {
        cleanupService.startCleanup(base, user, pass, projectId);

        // Poll until done
        while (cleanupService.isRunning()) {
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            if (stopFlag.get()) { cleanupService.stop(); break; }
            // Update DB progress
            TestHistory h = historyRepo.findByTaskId(tid);
            if (h != null) {
                h.setProgress(cleanupService.getProgress());
                h.setProgressTotal(cleanupService.getProgressTotal());
                historyRepo.save(h);
            }
        }

        TestHistory h = historyRepo.findByTaskId(tid);
        if (h != null) {
            h.setStatus(cleanupService.getStatus());
            h.setErrorMessage(cleanupService.getMsg());
            h.setOutput(cleanupService.getLog());
            h.setProgress(cleanupService.getProgress());
            h.setProgressTotal(cleanupService.getProgressTotal());
            historyRepo.save(h);
        }
    }

    // ── Private helpers ──

    private static String resolveLabel(TestRunRequest req) {
        if (req.testClass != null && !req.testClass.isBlank()) return req.testClass;
        if (req.module != null && !req.module.isBlank() && !"ALL".equals(req.module)) return req.module;
        return "ALL";
    }

    private String resolveProjectId(String projectId) {
        if (projectId != null && !projectId.isBlank()) return projectId;
        List<TestConfigEntity> cfs = configRepo.findAll();
        if (!cfs.isEmpty()) {
            TestConfigEntity c = cfs.get(0);
            if (c.getProjectId() != null && !c.getProjectId().isBlank()) return c.getProjectId();
        }
        return null;
    }
}
