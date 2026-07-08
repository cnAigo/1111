package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestHistory;
import org.example.testvue.entity.TestClassStats;
import org.example.testvue.entity.TestCaseDetail;
import org.example.testvue.repository.TestClassStatsRepository;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles Maven test process execution — stateless, concurrency-safe.
 * All per-task mutable state (Process, stop-flag) is held by the caller
 * via {@link TestExecutionService.TestTaskContext}.
 *
 * Logs are streamed directly to disk — never buffered in memory.
 */
@Component
public class MavenTestRunner {

    private static final Logger log = LoggerFactory.getLogger(MavenTestRunner.class);
    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");
    private static final Path LOG_DIR = Paths.get("logs");

    /**
     * Directory containing manual test scripts.
     * Scanned dynamically before every run to derive an accurate
     * {@code progressTotal} — avoids stale DB counts and ensures
     * the progress denominator matches what Maven will actually execute.
     */
    private final Path manualDir;

    private final TestHistoryRepository historyRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final TestClassStatsRepository statsRepo;
    private final WebSocketSessionManager wsManager;

    public MavenTestRunner(TestHistoryRepository historyRepo,
                           TestCaseDetailRepository caseDetailRepo,
                           TestClassStatsRepository statsRepo,
                           WebSocketSessionManager wsManager) {
        this.historyRepo = historyRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.statsRepo = statsRepo;
        this.wsManager = wsManager;
        this.manualDir = Paths.get(System.getProperty("user.dir"), "src", "test", "java", "cases", "manual");
        try { Files.createDirectories(LOG_DIR); } catch (Exception ignored) {}
    }

    /** Safe file read: tries UTF-8 first, falls back to GBK on MalformedInputException. */
    private static String readFileSafe(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            try {
                return Files.readString(p, Charset.forName("GBK"));
            } catch (IOException ex) {
                return "读取日志失败 (GBK): " + ex.getMessage();
            }
        } catch (IOException e) {
            return "读取日志失败: " + e.getMessage();
        }
    }

    /** Extract log content from disk file by taskId. Returns empty if file doesn't exist. */
    public static String readLog(String taskId) {
        Path p = LOG_DIR.resolve(taskId + ".log");
        if (Files.exists(p)) return readFileSafe(p);
        return "";
    }

    /**
     * Count the total number of test methods that will be executed.
     *
     * Dynamic-scan-first strategy: reads @Test annotations directly from
     * .java source files under src/test/java/cases/manual/.  This gives the
     * frontend an accurate progress denominator before Maven even starts,
     * and it never goes stale (unlike DB-derived stats).
     *
     * Falls back to DB records only when the source file cannot be found
     * (e.g. for non-manual test classes or legacy classpaths).
     */
    public int countTestCases(TestRunRequest req) {
        if (req.testClass != null && !req.testClass.isBlank()) {
            int count = 0;
            for (String cls : req.testClass.split(",")) {
                String cn = cls.trim();
                if (cn.isEmpty()) continue;
                // Use short class name: input may be cases.manual.Foo or just Foo
                String shortName = cn.contains(".") ? cn.substring(cn.lastIndexOf('.') + 1) : cn;
                Path srcFile = manualDir.resolve(shortName + ".java");
                if (Files.exists(srcFile)) {
                    count += countTestAnnotations(srcFile);
                } else {
                    // Fallback to DB for classes not in the manual directory
                    var stats = statsRepo.findByClassName(cn);
                    if (stats.isPresent()) count += stats.get().getMethodCount();
                    else count += caseDetailRepo.findByClassName(cn).size();
                }
            }
            return Math.max(count, 1);
        }

        // No specific class requested — scan every .java file under manual/
        int total = 0;
        if (Files.isDirectory(manualDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(manualDir, "*.java")) {
                for (Path f : ds) total += countTestAnnotations(f);
            } catch (IOException ignored) {}
        }
        if (total > 0) return total;

        // Last-resort fallback: aggregate stored DB counts
        for (var s : statsRepo.findAll()) total += s.getMethodCount();
        if (total > 0) return total;
        for (var d : caseDetailRepo.findAll()) {
            if (d.getClassName() != null && !d.getClassName().isBlank()) total++;
        }
        return Math.max(total, 1);
    }

    /**
     * Count the number of test classes that will be executed.
     * Used as the progress-bar denominator so the bar advances once per class.
     */
    public int countTestClasses(TestRunRequest req) {
        if (req.testClass != null && !req.testClass.isBlank()) {
            return (int) java.util.Arrays.stream(req.testClass.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .count();
        }
        if (Files.isDirectory(manualDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(manualDir, "*.java")) {
                int count = 0;
                for (Path f : ds) count++;
                return Math.max(count, 1);
            } catch (IOException ignored) {}
        }
        return 1;
    }

    /**
     * Count @Test-annotated methods in a single Java source file.
     * Only counts bare {@code @Test} annotations (after trimming) so it
     * ignores parameterized {@code @Test(dataProvider=...)} declarations —
     * this matches the counting convention used by ModuleScanner.
     */
    private static int countTestAnnotations(Path file) {
        try {
            String content = Files.readString(file);
            int count = 0;
            for (String line : content.split("\\r?\\n")) {
                if (line.trim().equals("@Test")) count++;
            }
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Extract the simple class name from a surefire summary line.
     * Input:  {@code Tests run: 5 ... -- in com.example.cases.manual.FooTest}
     * Output: {@code FooTest}
     */
    private static String extractClassFromTestsRunLine(String line) {
        int idx = line.lastIndexOf(" -- in ");
        if (idx < 0) return null;
        String fullName = line.substring(idx + 6).trim();
        int dot = fullName.lastIndexOf('.');
        return dot >= 0 ? fullName.substring(dot + 1) : fullName;
    }

    // ── Process tree kill (static utility) ──

    /**
     * Recursively kill a process and all its descendants.
     * Kills children first, then the parent — prevents orphan/zombie processes.
     */
    public static void killProcessTree(Process process) {
        if (process == null) return;
        ProcessHandle handle = process.toHandle();
        try {
            handle.descendants().forEach(child -> {
                try { child.destroyForcibly(); } catch (Exception ignored) {}
            });
        } catch (SecurityException ignored) {}
        try {
            process.destroyForcibly();
        } catch (Exception ignored) {}
    }

    /**
     * Terminate orphaned headless browser and Maven Surefire-fork processes
     * left behind by previous test runs that crashed or were force-stopped.
     *
     * <h3>Why this matters</h3>
     * Playwright launches headless Chromium/Firefox instances that consume
     * hundreds of MB of RAM each.  Maven Surefire forks a separate JVM per
     * test class (when {@code forkMode} is enabled).  If the parent Spring
     * Boot process is killed, restarted, or loses track of these children,
     * they become zombies that leak memory and CPU until the OS is rebooted.
     *
     * <h3>Safety — the "don't close my browser" guarantee</h3>
     * On <b>Windows</b> we use {@code wmic} to filter by command-line before
     * killing: only processes whose full command line contains
     * {@code --headless} (the flag Playwright passes to every launch) are
     * terminated.  Your regular Chrome/Edge/Firefox windows are never
     * touched.  If {@code wmic} is unavailable the cleanup logs a warning
     * and skips — we never fall back to a broad {@code taskkill /F /IM}.
     *
     * On <b>Linux / macOS</b> we use {@code pkill -f} with a regex that
     * already requires {@code headless} or {@code remote-debugging} in the
     * command line, plus the Surefire booter jar name.
     *
     * <p>This runs before every new Maven invocation so the server never
     * accumulates zombie processes across multiple test runs.
     */
    static void killOrphanedProcesses() {
        if (IS_WIN) {
            // Use WMIC to kill ONLY headless browser instances — filter by
            // the "--headless" flag that Playwright appends to every launch.
            // Regular user-facing browser windows never have this flag.
            for (String img : new String[]{"chrome.exe", "chromium.exe", "msedge.exe", "firefox.exe"}) {
                try {
                    int rc = new ProcessBuilder(
                            "wmic", "process", "where",
                            "name='" + img + "' and commandline like '%--headless%'",
                            "call", "terminate")
                            .redirectErrorStream(true)
                            .start()
                            .waitFor(10, TimeUnit.SECONDS) ? 0 : -1;
                    if (rc != 0) {
                        log.debug("wmic terminate returned {} for {} (no matching headless processes — ok)", rc, img);
                    }
                } catch (Exception e) {
                    // wmic may not exist on minimal Windows installations.
                    // We deliberately do NOT fall back to taskkill /F /IM
                    // because that would kill user-facing browser windows.
                    log.debug("Failed to run wmic for {}: {}", img, e.getMessage());
                }
            }
        } else {
            // Linux / macOS: pkill -f with a regex anchored to headless
            // flags so only Playwright-launched browsers are targeted.
            try {
                new ProcessBuilder("pkill", "-f",
                        "(chromium|chrome|msedge|firefox).*(headless|remote-debugging)")
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            // Maven Surefire forked JVMs carry "surefirebooter" on the classpath.
            try {
                new ProcessBuilder("pkill", "-f", "surefirebooter")
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }
    }

    // ── Run ──

    /**
     * Run Maven tests synchronously (called from an @Async executor).
     * All per-task state lives in the supplied {@code stopFlag} and the returned Process.
     */
    public void run(String tid, TestRunRequest req, String label,
                    AtomicBoolean stopFlag) throws Exception {
        long t0 = System.currentTimeMillis();
        String mvn = IS_WIN ? "mvnw.cmd" : "./mvnw";
        Path logFile = LOG_DIR.resolve(tid + ".log");

        int totalClasses = countTestClasses(req);
        int expectedMethods = countTestCases(req);

        // Reclaim memory and CPU from zombie processes left by a previous
        // run that may have crashed or been kill -9'd.  Must happen before
        // we spawn any new processes so the cleanup doesn't accidentally
        // target the current run's own children.
        killOrphanedProcesses();

        // Clean previous surefire reports — shared directory, use random sleep to reduce contention
        Path sfDir = Paths.get(System.getProperty("user.dir"), "target/surefire-reports");
        try {
            if (Files.exists(sfDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(sfDir, "TEST-*.xml")) {
                    for (Path f : ds) Files.deleteIfExists(f);
                }
            }
        } catch (Exception ignored) {}

        List<String> cmd = buildCommand(mvn, req);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);

        if (req.url != null && !req.url.isBlank()) pb.environment().put("BASE_URL", req.url);
        if (req.username != null && !req.username.isBlank()) pb.environment().put("TAAS_USER", req.username);
        if (req.password != null && !req.password.isBlank()) pb.environment().put("TAAS_PASS", req.password);
        if (req.projectId != null && !req.projectId.isBlank()) pb.environment().put("TAAS_PROJECT_ID", req.projectId);

        Process proc = pb.start();

        // Watchdog: kill process when stopFlag is set (even if blocked on readLine)
        Thread watchdog = new Thread(() -> {
            while (!stopFlag.get()) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
            }
            killProcessTree(proc);
        }, "watchdog-" + tid);
        watchdog.setDaemon(true);
        watchdog.start();

        // Stream output to disk AND push via WebSocket
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new FileWriter(logFile.toFile()))) {

            // Track which test classes have already been counted.
            // When Maven retries a failing test class (surefire
            // rerunFailingTestsCount > 0), the runner emits another
            // "Tests run: N ... -- in ClassName" line for that class.
            // Without deduplication `accumulated` would count the same
            // class twice — or more — causing the progress bar to blow
            // past 100%.
            Set<String> seenClasses = new HashSet<>();
            int completedMethods = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (stopFlag.get()) { killProcessTree(proc); break; }

                writer.write(line);
                writer.newLine();
                writer.flush();

                wsManager.pushLine(tid, line);

                // Push progress when a class starts running so frontend
                // knows execution is underway even before any class finishes
                if (line.contains("Running ") && line.trim().matches(".*Running\\s+[\\w.]+.*")) {
                    wsManager.pushProgress(tid, seenClasses.size(), totalClasses);
                }

                if (line.contains("Tests run:")) {
                    try {
                        int p = extractInt(line, "Tests run:");
                        if (line.contains(" -- in ")) {
                            String cls = extractClassFromTestsRunLine(line);
                            if (cls != null && seenClasses.add(cls)) {
                                completedMethods += p;
                            }
                        } else {
                            // Final aggregate line — all classes done
                            completedMethods = p;
                        }
                        int classesDone = seenClasses.size();
                        // If only 1 class, it may not have been detected by
                        // extractClassFromTestsRunLine — ensure progress > 0
                        if (classesDone == 0 && !line.contains(" -- in ")) classesDone = totalClasses;
                        updateProgress(tid, completedMethods);
                        log.info("Progress: {}/{} classes, {}/{} methods", classesDone, totalClasses, completedMethods, expectedMethods);
                        wsManager.pushProgress(tid, classesDone, totalClasses);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Wait with periodic stopFlag check
        boolean finished = false;
        int waitedSeconds = 0;
        while (!finished && waitedSeconds < 1800) {
            finished = proc.waitFor(2, TimeUnit.SECONDS);
            if (stopFlag.get()) {
                killProcessTree(proc);
                finished = true;
            }
            waitedSeconds += 2;
        }
        int exitCode = finished ? proc.exitValue() : -1;
        if (stopFlag.get()) {
            updateStatus(tid, "STOPPED", "手动停止");
            wsManager.pushStopped(tid);
            return;
        }

        if (!finished) {
            updateStatus(tid, "FAILED", "执行超时 (30分钟)");
            return;
        }

        long durMs = System.currentTimeMillis() - t0;

        // ── Post-run: parse + report ──
        wsManager.pushLine(tid, "");
        wsManager.pushLine(tid, "测试已完成，正在解析结果...");

        List<ClassResult> results = SurefireParser.parseDir(sfDir);
        int totalRun = 0, failed = 0, passed = 0;
        for (ClassResult r : results) {
            totalRun += r.tests;
            failed += r.failures + r.errors;
            passed += r.tests - r.failures - r.errors - r.skipped;
        }

        String rawLog = readFileSafe(logFile);
        extractWarnings(results, rawLog);
        attachClassLogs(results, rawLog);

        wsManager.pushLine(tid, "正在生成测试报告...");
        try {
            List<String> ac = new ArrayList<>();
            ac.add(mvn); ac.add("allure:report"); ac.add("-q");
            new ProcessBuilder(ac).directory(new File(System.getProperty("user.dir")))
                .start().waitFor(30, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        wsManager.pushLine(tid, "报告生成完成，正在保存...");

        String resultJson = new com.google.gson.Gson().toJson(results);

        String status;
        String statusMsg;
        if (totalRun == 0 && exitCode != 0) { status = "FAILED"; statusMsg = "mvn test 失败 (exit=" + exitCode + ")"; }
        else if (totalRun == 0) { status = "FAILED"; statusMsg = "未发现匹配的测试用例"; }
        else if (failed > 0) { status = "FAILED"; statusMsg = label + " — " + failed + "/" + totalRun + " failed"; }
        else { status = "SUCCESS"; statusMsg = label + " — all " + totalRun + " passed"; }

        TestHistory h = historyRepo.findByTaskId(tid);
        if (h != null) {
            h.setStatus(status);
            h.setErrorMessage(statusMsg);
            h.setDurationMs(durMs);
            h.setDurationFmt(fmt(durMs));
            h.setPassed(passed);
            h.setFailed(failed);
            h.setProgress(totalRun);
            h.setProgressTotal(totalRun);
            h.setOutput(statusMsg);
            h.setResultJson(resultJson);
            h.setLogFilePath(logFile.toString());
            historyRepo.save(h);
        }

        updateClassStats(results);
        wsManager.pushResult(tid, status, fmt(durMs), statusMsg, totalRun, totalRun);
    }

    // ── DB helpers ──

    private void updateProgress(String tid, int p) {
        TestHistory h = historyRepo.findByTaskId(tid);
        if (h != null) { h.setProgress(p); historyRepo.save(h); }
    }

    private void updateStatus(String tid, String status, String msg) {
        TestHistory h = historyRepo.findByTaskId(tid);
        if (h != null) {
            h.setStatus(status);
            h.setErrorMessage(msg);
            h.setOutput(msg);
            historyRepo.save(h);
        }
    }

    // ── Build command ──

    private List<String> buildCommand(String mvn, TestRunRequest req) {
        List<String> cmd = new ArrayList<>();
        cmd.add(mvn); cmd.add("test");
        if (req.testClass != null && !req.testClass.isBlank()) {
            cmd.add("-Dtest=" + req.testClass);
        } else {
            cmd.add("-Dtest=cases.manual.*");
        }
        cmd.add("-DfailIfNoTests=false");
        cmd.add("-Dplaywright.headless=true");
        cmd.add("-Dstyle.color=always");
        return cmd;
    }

    private static int extractInt(String s, String prefix) {
        int i = s.indexOf(prefix);
        if (i < 0) return 0;
        String sub = s.substring(i + prefix.length()).trim();
        int end = 0;
        while (end < sub.length() && Character.isDigit(sub.charAt(end))) end++;
        return Integer.parseInt(sub.substring(0, end));
    }

    // ── Warning / log extraction (unchanged logic) ──

    private void extractWarnings(List<ClassResult> results, String rawLog) {
        if (rawLog == null || rawLog.isEmpty() || results.isEmpty()) return;
        String[] lines = rawLog.split("\\r?\\n");
        String currentClass = null;
        StringBuilder currentSection = new StringBuilder();
        for (String line : lines) {
            if (line.contains("Running ") && (line.contains("[INFO]") || line.trim().startsWith("Running"))) {
                if (currentClass != null && currentSection.length() > 0) {
                    attachWarningsToClass(results, currentClass, currentSection.toString());
                }
                String s = line.replaceAll(".*Running\\s+", "").trim();
                int dot = s.lastIndexOf('.');
                currentClass = dot >= 0 ? s.substring(dot + 1) : s;
                currentSection.setLength(0);
            }
            if (currentClass != null) {
                currentSection.append(line).append('\n');
            }
        }
        if (currentClass != null && currentSection.length() > 0) {
            attachWarningsToClass(results, currentClass, currentSection.toString());
        }
    }

    private void attachClassLogs(List<ClassResult> results, String rawLog) {
        if (rawLog == null || rawLog.isEmpty()) return;
        String[] lines = rawLog.split("\\r?\\n");
        String currentClass = null;
        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            if (line.contains("Running ") && (line.contains("[INFO]") || line.trim().startsWith("Running"))) {
                if (currentClass != null && buf.length() > 0) {
                    setClassLog(results, currentClass, buf.toString());
                }
                String s = line.replaceAll(".*Running\\s+", "").trim();
                int dot = s.lastIndexOf('.');
                currentClass = dot >= 0 ? s.substring(dot + 1) : s;
                buf.setLength(0);
                continue;
            }
            if (currentClass != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;
                if (t.startsWith("[INFO] ---") || t.startsWith("[INFO] Building")
                    || t.startsWith("[INFO] -------") || t.startsWith("[INFO] >>>")
                    || t.startsWith("[INFO] <<<") || t.startsWith("[DEBUG]")
                    || t.startsWith("[WARNING]") || t.startsWith("Downloading")
                    || t.startsWith("Downloaded") || t.startsWith("Progress"))
                    continue;
                boolean appLine = t.matches("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*")
                    || t.matches("^\\[INFO\\]\\s+Tests run:.*")
                    || t.matches("^\\[INFO\\]\\s+\\d+.*")
                    || t.startsWith("Tests run:");
                if (appLine) {
                    String clean = t.replaceFirst("^\\[INFO\\]\\s+", "");
                    buf.append(clean).append('\n');
                }
            }
        }
        if (currentClass != null && buf.length() > 0) {
            setClassLog(results, currentClass, buf.toString());
        }
    }

    private void setClassLog(List<ClassResult> results, String className, String log) {
        for (ClassResult r : results)
            if (r.className.equals(className)) { r.log = log; return; }
    }

    private void attachWarningsToClass(List<ClassResult> results, String className, String section) {
        ClassResult target = null;
        for (ClassResult r : results) {
            if (r.className.equals(className)) { target = r; break; }
        }
        if (target == null) return;
        List<String> warns = new ArrayList<>();
        for (String line : section.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            boolean isWarn = (t.contains(" WARN ") || t.contains(" WARNING ")
                           || t.matches(".*\\[WARN(ING)?\\].*"))
                           && !t.startsWith("[ERROR] Failed to execute")
                           && !t.matches(".*\\[ERROR\\]\\s*$")
                           && !t.startsWith("\tat ")
                           && !t.startsWith("Caused by:");
            boolean isError = t.contains(" ERROR ") && !t.startsWith("[ERROR]") && !t.startsWith("\tat ");
            if (isWarn || isError) {
                String clean = t.replaceFirst("^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+\\[[^]]+\\]\\s+", "");
                if (clean.length() > 300) clean = clean.substring(0, 300) + "...";
                warns.add(clean);
            }
        }
        if (!warns.isEmpty()) { target.warnings = warns; }
    }

    public static String fmt(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000, m = s / 60;
        s %= 60;
        return m > 0 ? m + "m" + s + "s" : s + "s";
    }

    // ── Duration stats ──

    public long calculateExpectedMs(TestRunRequest req) {
        if (req.testClass != null && !req.testClass.isBlank()) {
            long total = 0;
            for (String cls : req.testClass.split(",")) {
                String cn = cls.trim();
                if (cn.isEmpty()) continue;
                var stats = statsRepo.findByClassName(cn);
                if (stats.isPresent() && stats.get().getAvgDurationMs() > 0)
                    total += stats.get().getAvgDurationMs();
            }
            return total;
        }
        long total = 0;
        for (var s : statsRepo.findAll()) {
            if (s.getAvgDurationMs() > 0) total += s.getAvgDurationMs();
        }
        return total;
    }

    private void updateClassStats(List<ClassResult> results) {
        for (ClassResult cr : results) {
            if (cr.className == null || cr.time == null) continue;
            double sec;
            try { sec = Double.parseDouble(cr.time); } catch (NumberFormatException e) { continue; }
            if (sec <= 0) continue;
            long durationMs = Math.round(sec * 1000);
            int methodCount = cr.tests;
            var opt = statsRepo.findByClassName(cr.className);
            TestClassStats s;
            if (opt.isPresent()) {
                s = opt.get();
                if (s.getAvgDurationMs() > 0 && s.getSampleCount() > 0) {
                    long avg = s.getAvgDurationMs();
                    if (durationMs > avg * 3 || durationMs < avg / 3) {
                        s.setMethodCount(methodCount);
                        statsRepo.save(s);
                        continue;
                    }
                }
                s.setMethodCount(methodCount);
                int n = Math.min(s.getSampleCount(), 5);
                s.setAvgDurationMs((s.getAvgDurationMs() * n + durationMs) / (n + 1));
                s.setSampleCount(Math.min(s.getSampleCount() + 1, 5));
            } else {
                s = new TestClassStats(cr.className, methodCount, durationMs, 1);
            }
            statsRepo.save(s);
        }
    }
}
