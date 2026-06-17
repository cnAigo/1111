package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestConfigEntity;
import org.example.testvue.entity.TestHistory;
import org.example.testvue.repository.TestConfigRepository;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates test execution — supports concurrent task runs.
 * Each task owns its own {@link TestTaskContext} (stop-flag, process ref, status).
 * All state is persisted to DB via TestHistory.
 */
@Service
public class TestExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TestExecutionService.class);

    // ── Per-task context (thread-safe) ──

    public static class TestTaskContext {
        public final String taskId;
        public final String label;
        public final AtomicBoolean stopFlag = new AtomicBoolean(false);
        public volatile String status = "RUNNING";
        public volatile String errorMessage;
        public volatile int progress;
        public volatile int progressTotal;
        public final long startTime = System.currentTimeMillis();

        public TestTaskContext(String taskId, String label) {
            this.taskId = taskId;
            this.label = label;
        }
    }

    // ── Dependencies ──

    private final TestHistoryRepository historyRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final TestConfigRepository configRepo;
    private final MavenTestRunner mavenRunner;
    private final CleanupService cleanupService;
    private final WebSocketSessionManager wsManager;
    private final ApplicationContext appCtx;

    /** Concurrent tasks keyed by taskId */
    private final ConcurrentHashMap<String, TestTaskContext> tasks = new ConcurrentHashMap<>();

    public TestExecutionService(TestHistoryRepository historyRepo,
                                TestCaseDetailRepository caseDetailRepo,
                                TestConfigRepository configRepo,
                                MavenTestRunner mavenRunner,
                                CleanupService cleanupService,
                                WebSocketSessionManager wsManager,
                                ApplicationContext appCtx) {
        this.historyRepo = historyRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.configRepo = configRepo;
        this.mavenRunner = mavenRunner;
        this.cleanupService = cleanupService;
        this.wsManager = wsManager;
        this.appCtx = appCtx;
    }

    @PostConstruct
    public void fixStaleRunningStatus() {
        TestHistory h = historyRepo.findTopByOrderByCreateTimeDesc();
        if (h != null && "RUNNING".equals(h.getStatus())) {
            h.setStatus("STOPPED");
            h.setErrorMessage("服务重启，任务中断");
            historyRepo.save(h);
            LOG.info("Fixed stale RUNNING status for taskId={}", h.getTaskId());
        }
    }

    // ── Read helpers ──

    private TestHistory getCurrent() {
        return historyRepo.findTopByOrderByCreateTimeDesc();
    }

    /** Check if ANY task is currently running. */
    public boolean isRunning() {
        for (TestTaskContext ctx : tasks.values()) {
            if ("RUNNING".equals(ctx.status)) return true;
        }
        return false;
    }

    /** Check if a specific task is running. */
    public boolean isRunning(String taskId) {
        TestTaskContext ctx = tasks.get(taskId);
        return ctx != null && "RUNNING".equals(ctx.status);
    }

    /** Total number of active (RUNNING) tasks. */
    public int runningTaskCount() {
        int count = 0;
        for (TestTaskContext ctx : tasks.values()) {
            if ("RUNNING".equals(ctx.status)) count++;
        }
        return count;
    }

    // ── Status (per-task, with taskId-before-global fallback) ──

    /** Get status for a specific taskId, or the most recent record if taskId is null. */
    public StatusResponse getStatus(String taskId) {
        if (taskId != null && !taskId.isBlank()) {
            TestTaskContext ctx = tasks.get(taskId);
            if (ctx != null) {
                StatusResponse s = new StatusResponse();
                s.taskId = ctx.taskId;
                s.status = ctx.status;
                s.label = ctx.label;
                s.errorMessage = ctx.errorMessage != null ? ctx.errorMessage : "";
                s.progress = ctx.progress;
                s.progressTotal = ctx.progressTotal;
                s.msg = "";
                // Read log from disk
                s.output = MavenTestRunner.readLog(taskId);
                return s;
            }
            // Fall back to DB
            TestHistory h = historyRepo.findByTaskId(taskId);
            if (h != null) return buildStatusFromHistory(h);
            StatusResponse s = new StatusResponse();
            s.status = "IDLE"; s.label = ""; s.durationFmt = "";
            return s;
        }
        return getCurrentStatus();
    }

    /** Status of the most recent task (backward compat). */
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
            s.estimatedMs = h.getEstimatedMs();
            s.errorMessage = h.getErrorMessage() != null ? h.getErrorMessage() : "";
            s.taskId = h.getTaskId();
            s.output = MavenTestRunner.readLog(h.getTaskId());
        } else {
            s.status = "IDLE"; s.label = ""; s.durationFmt = "";
        }
        return s;
    }

    private StatusResponse buildStatusFromHistory(TestHistory h) {
        StatusResponse s = new StatusResponse();
        s.taskId = h.getTaskId();
        s.status = h.getStatus() != null ? h.getStatus() : "IDLE";
        s.msg = h.getErrorMessage() != null ? h.getErrorMessage() : "";
        s.label = h.getLabel() != null ? h.getLabel() : "";
        s.durationMs = h.getDurationMs();
        s.durationFmt = h.getDurationFmt() != null ? h.getDurationFmt() : "";
        s.progress = h.getProgress();
        s.progressTotal = h.getProgressTotal();
        s.estimatedMs = h.getEstimatedMs();
        s.errorMessage = h.getErrorMessage() != null ? h.getErrorMessage() : "";
        s.output = MavenTestRunner.readLog(h.getTaskId());
        return s;
    }

    // ── Start ──

    /** Start a new test run. Returns taskId, or null if the specific classes are already running. */
    public synchronized String startRun(TestRunRequest req) {
        String tid = UUID.randomUUID().toString();
        String lb = resolveLabel(req);

        // Check for duplicate: same test class already running?
        if (req.testClass != null && !req.testClass.isBlank()) {
            for (TestTaskContext ctx : tasks.values()) {
                if ("RUNNING".equals(ctx.status) && lb.equals(ctx.label)) return null;
            }
        }

        int total = mavenRunner.countTestCases(req);
        if (total <= 0) total = 1;

        long estimatedMs = mavenRunner.calculateExpectedMs(req);

        TestHistory h = TestHistory.of(tid, lb, "RUNNING", "", 0, 0, 0, "", "");
        h.setProgress(0);
        h.setProgressTotal(total);
        h.setEstimatedMs(estimatedMs);
        historyRepo.save(h);

        TestTaskContext ctx = new TestTaskContext(tid, lb);
        ctx.progressTotal = total;
        tasks.put(tid, ctx);

        // Push initial 0/total so frontend shows progress bar from the start
        wsManager.pushProgress(tid, 0, total);

        appCtx.getBean(TestExecutionService.class).executeRunAsync(tid, req, lb);
        return tid;
    }

    @Async("testExecutor")
    public void executeRunAsync(String tid, TestRunRequest req, String label) {
        try {
            mavenRunner.run(tid, req, label, tasks.get(tid).stopFlag);
        } catch (Exception e) {
            LOG.error("Test execution failed for taskId={}", tid, e);
            TestHistory h = historyRepo.findByTaskId(tid);
            if (h != null) {
                h.setStatus("FAILED");
                h.setErrorMessage(e.getMessage());
                h.setOutput(e.getMessage());
                historyRepo.save(h);
            }
        } finally {
            TestTaskContext ctx = tasks.get(tid);
            if (ctx != null) ctx.status = "COMPLETED";
        }
    }

    // ── Stop ──

    /** Stop the most recent running task (backward compat — stops all). */
    public synchronized void stopRun() {
        for (TestTaskContext ctx : tasks.values()) {
            if ("RUNNING".equals(ctx.status)) {
                stopRun(ctx.taskId);
            }
        }
    }

    /** Stop a specific task by taskId. */
    public synchronized void stopRun(String taskId) {
        TestTaskContext ctx = tasks.get(taskId);
        if (ctx == null || !"RUNNING".equals(ctx.status)) return;

        ctx.stopFlag.set(true);
        cleanupService.stop();

        // Update DB
        TestHistory h = historyRepo.findByTaskId(taskId);
        if (h != null && "RUNNING".equals(h.getStatus())) {
            h.setStatus("STOPPED");
            h.setErrorMessage("手动停止");
            historyRepo.save(h);
        }
        ctx.status = "STOPPED";
    }

    // ── Cleanup ──

    public synchronized String startCleanup(String projectId, String url,
                                            String username, String password) {
        if (isRunning()) return null;
        String pId = resolveProjectId(projectId);
        String base = (url != null && !url.isBlank()) ? url : "https://192.168.6.171:8088";
        String user = (username != null && !username.isBlank()) ? username : "admin";
        String pass = (password != null && !password.isBlank()) ? password : "Aa123456";

        String tid = "cleanup-" + System.currentTimeMillis();
        TestHistory h = TestHistory.of(tid, "清理环境", "RUNNING", "", 0, 0, 0, "", "");
        h.setProgress(0); h.setProgressTotal(0);
        historyRepo.save(h);

        TestTaskContext ctx = new TestTaskContext(tid, "清理环境");
        tasks.put(tid, ctx);

        appCtx.getBean(TestExecutionService.class).runCleanupAsync(base, user, pass, pId, tid);
        return tid;
    }

    @Async("testExecutor")
    public void runCleanupAsync(String base, String user, String pass, String projectId, String tid) {
        cleanupService.startCleanup(base, user, pass, projectId, tid, wsManager);

        TestTaskContext ctx = tasks.get(tid);
        AtomicBoolean stopFlag = ctx != null ? ctx.stopFlag : new AtomicBoolean(false);

        while (cleanupService.isRunning()) {
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            if (stopFlag.get()) { cleanupService.stop(); break; }
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
        if (ctx != null) ctx.status = "COMPLETED";
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
