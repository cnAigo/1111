package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestConfigEntity;
import org.example.testvue.repository.TestConfigRepository;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates test execution and environment cleanup.
 * Delegates heavy lifting to {@link MavenTestRunner} and {@link CleanupService}.
 */
@Service
public class TestExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TestExecutionService.class);

    private final TestHistoryRepository historyRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final TestConfigRepository configRepo;
    private final CleanupService cleanupService;

    private volatile String status = "IDLE";
    private volatile String msg = "";
    private volatile String label = "";
    private volatile long durMs = 0;
    private volatile int progress = 0;
    private volatile int progressTotal = 0;
    private volatile String currentTaskId;
    private final StringBuilder logBuf = new StringBuilder();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    // Lazy-created — needs the shared stopFlag and logBuf
    private MavenTestRunner mavenRunner;

    public TestExecutionService(TestHistoryRepository historyRepo,
                                TestCaseDetailRepository caseDetailRepo,
                                TestConfigRepository configRepo,
                                CleanupService cleanupService) {
        this.historyRepo = historyRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.configRepo = configRepo;
        this.cleanupService = cleanupService;
    }

    // ── Run ──

    public synchronized String startRun(TestRunRequest req) {
        if ("RUNNING".equals(status)) return null;
        stopFlag.set(false);
        String tid = UUID.randomUUID().toString();
        String lb = resolveLabel(req);

        status = "RUNNING"; msg = ""; label = lb; durMs = 0;
        progress = 0; currentTaskId = tid;
        synchronized (logBuf) { logBuf.setLength(0); }

        if (mavenRunner == null)
            mavenRunner = new MavenTestRunner(historyRepo, caseDetailRepo, stopFlag, logBuf);

        progressTotal = mavenRunner.countTestCases(req);
        if (progressTotal <= 0) progressTotal = 1;

        new Thread(() -> mavenRunner.run(tid, req, lb, new MavenTestRunner.StatusUpdater() {
            public void updateProgress(int p) { progress = p; }
            public void stopped() { status = "STOPPED"; }
            public void finished(String s, String m, long d, int total) {
                status = s; msg = m; durMs = d; progress = total; progressTotal = total;
            }
            public void failed(String s, String m, long d) {
                status = s; msg = m; durMs = d;
            }
        })).start();
        return tid;
    }

    public void stopRun() {
        stopFlag.set(true);
        if (mavenRunner != null) mavenRunner.kill();
        cleanupService.stop();
        status = "IDLE"; msg = "手动停止";
    }

    public StatusResponse getCurrentStatus() {
        StatusResponse s = new StatusResponse();
        s.status = status; s.msg = msg; s.label = label;
        s.durationMs = durMs; s.durationFmt = MavenTestRunner.fmt(durMs);
        s.progress = progress; s.progressTotal = progressTotal;
        s.errorMessage = msg; s.taskId = currentTaskId;
        synchronized (logBuf) { s.output = logBuf.toString(); }
        return s;
    }

    public boolean isRunning() { return "RUNNING".equals(status); }

    // ── Cleanup ──

    public synchronized void startCleanup(String projectId, String url,
                                           String username, String password) {
        if ("RUNNING".equals(status)) return;
        stopFlag.set(false);
        status = "RUNNING"; msg = ""; label = "清理环境";
        durMs = 0; progress = 0; progressTotal = 0;
        synchronized (logBuf) { logBuf.setLength(0); }
        currentTaskId = "cleanup";

        String pId = resolveProjectId(projectId);
        String base = (url != null && !url.isBlank()) ? url : "https://192.168.6.171:8088";
        String user = (username != null && !username.isBlank()) ? username : "admin";
        String pass = (password != null && !password.isBlank()) ? password : "Aa123456";

        cleanupService.startCleanup(base, user, pass, pId);
        new Thread(() -> pollCleanup()).start();
    }

    private void pollCleanup() {
        while (cleanupService.isRunning()) {
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            if (stopFlag.get()) { cleanupService.stop(); break; }
        }
        status = cleanupService.getStatus();
        msg = cleanupService.getMsg();
        progress = cleanupService.getProgress();
        progressTotal = cleanupService.getProgressTotal();
        durMs = 0;
        synchronized (logBuf) { logBuf.setLength(0); logBuf.append(cleanupService.getLog()); }
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
