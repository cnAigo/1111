package org.example.testvue.controller;

import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.engine.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

@RestController
@RequestMapping("/api/test")
public class TestRunnerController {

    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private volatile String status = "READY";
    private volatile String msg = "";
    private volatile String label = "";
    private volatile long durMs = 0;
    private volatile int progress = 0;
    private volatile int progressTotal = 0;

    private final StringBuilder logBuf = new StringBuilder();
    private final Map<String, TestExecutionSummary> taskResults = new ConcurrentHashMap<>();
    private volatile String currentTaskId;

    // Cache simple class name -> FQN mapping
    private volatile Map<String, String> classIndex;

    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam(value = "taskId", required = false) String taskId) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (taskId != null) m.put("taskId", taskId);
        m.put("status", status);
        m.put("msg", msg);
        m.put("label", label);
        m.put("durationMs", durMs);
        m.put("durationFmt", fmt(durMs));
        m.put("progress", progress);
        m.put("progressTotal", progressTotal);
        m.put("errorMessage", msg);
        synchronized (logBuf) { m.put("output", logBuf.toString()); }
        return m;
    }

    @GetMapping("/report-summary")
    public Map<String, Object> reportSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Path p = Paths.get(System.getProperty("user.dir"),
                "target/site/allure-maven-plugin/widgets/summary.json");
            if (!Files.exists(p)) {
                m.put("error", "report not found");
                return m;
            }
            String raw = Files.readString(p);
            m.put("total",   extractInt(raw, "total"));
            m.put("passed",  extractInt(raw, "passed"));
            int f = extractInt(raw, "failed");
            int b = extractInt(raw, "broken");
            m.put("failed",  f + b);
            m.put("skipped", extractInt(raw, "skipped"));
            m.put("timeSec", extractInt(raw, "sumDuration") / 1000);
        } catch (Exception e) {
            m.put("error", e.getMessage());
        }
        return m;
    }

    private int extractInt(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return 0;
        i = json.indexOf(":", i) + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) i++;
        int end = i;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(i, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    @GetMapping("/results")
    public List<Map<String, Object>> testResults() {
        return readSurefireXml();
    }

    private List<Map<String, Object>> readSurefireXml() {
        List<Map<String, Object>> list = new ArrayList<>();
        Path dir = Paths.get(System.getProperty("user.dir"), "target/surefire-reports");
        if (!Files.exists(dir)) return list;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "TEST-*.xml")) {
            for (Path f : ds) {
                String xml = Files.readString(f);
                Map<String, Object> cls = new LinkedHashMap<>();
                cls.put("className", shortName(attr(xml, "testsuite", "name")));
                cls.put("tests",    Integer.parseInt(attr(xml, "testsuite", "tests")));
                cls.put("failures", Integer.parseInt(attr(xml, "testsuite", "failures")));
                cls.put("errors",   Integer.parseInt(attr(xml, "testsuite", "errors")));
                cls.put("skipped",  Integer.parseInt(attr(xml, "testsuite", "skipped")));
                cls.put("time",     attr(xml, "testsuite", "time"));

                List<Map<String, String>> cases = new ArrayList<>();
                int idx = 0;
                while ((idx = xml.indexOf("<testcase ", idx)) >= 0) {
                    int tagEnd = xml.indexOf(">", idx);
                    int closeIdx = xml.indexOf("</testcase>", tagEnd);
                    if (closeIdx < 0) closeIdx = xml.indexOf("/>", tagEnd);
                    String full = closeIdx > tagEnd ? xml.substring(idx, closeIdx) : xml.substring(idx, tagEnd + 1);
                    String name = attr(full, "testcase", "name");
                    String tm = attr(full, "testcase", "time");
                    boolean fail = full.contains("<failure");
                    boolean err  = full.contains("<error") && !full.contains("<error ");

                    String reason = "";
                    if (fail || err) {
                        String ftag = fail ? "failure" : "error";
                        int fi = full.indexOf("<" + ftag);
                        if (fi >= 0) {
                            reason = attr(full.substring(fi), ftag, "message");
                            reason = reason.replace("&lt;", "<").replace("&gt;", ">")
                                           .replace("&amp;", "&").replace("&quot;", "\"");
                            int cd = full.indexOf("<![CDATA[", fi);
                            if (cd >= 0) {
                                int ce = full.indexOf("]]>", cd);
                                if (ce > cd) {
                                    String cdata = full.substring(cd + 9, ce).trim();
                                    String[] lines = cdata.split("\\r?\\n");
                                    StringBuilder sb = new StringBuilder();
                                    int taken = 0;
                                    for (String ln : lines) {
                                        if (ln.trim().isEmpty()) continue;
                                        sb.append(ln.trim()).append("\n");
                                        if (++taken >= 4) break;
                                    }
                                    reason = reason + "\n" + sb.toString().trim();
                                }
                            }
                        }
                    }

                    Map<String, String> c = new LinkedHashMap<>();
                    c.put("name", name);
                    c.put("time", tm);
                    c.put("status", fail || err ? "FAIL" : "PASS");
                    c.put("reason", reason);
                    cases.add(c);
                    idx = closeIdx > 0 ? closeIdx + 1 : tagEnd + 1;
                }
                cls.put("cases", cases);
                list.add(cls);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String attr(String xml, String tag, String attr) {
        String prefix = "<" + tag + " ";
        int i = xml.indexOf(prefix);
        if (i < 0) return "";
        String s = xml.substring(i + prefix.length());
        String key = attr + "=\"";
        int ki = s.indexOf(key);
        if (ki < 0) return "";
        int vs = ki + key.length();
        int ve = s.indexOf("\"", vs);
        return ve > vs ? s.substring(vs, ve) : "";
    }

    private String shortName(String full) {
        if (full == null || full.isEmpty()) return "";
        int dot = full.lastIndexOf('.');
        return dot >= 0 ? full.substring(dot + 1) : full;
    }

    // ==================== Run Tests (in-process JUnit Launcher) ====================

    @PostMapping("/run")
    public Map<String, Object> runTests(@RequestBody Map<String, String> req) {
        if ("RUNNING".equals(status)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("code", 409); r.put("msg", "已有任务正在执行中，请等待完成后再试");
            return r;
        }

        String testClass = req.getOrDefault("testClass", "");
        String module = req.getOrDefault("module", "");
        String taskId = UUID.randomUUID().toString();

        String lb;
        if (!testClass.isBlank()) lb = testClass;
        else if (!module.isBlank() && !"ALL".equals(module)) lb = module;
        else lb = "ALL";

        final String tc = testClass, mod = module, labelF = lb, tid = taskId;
        status = "RUNNING"; msg = ""; label = labelF; durMs = 0; progress = 0;
        progressTotal = 0; currentTaskId = tid;
        synchronized (logBuf) { logBuf.setLength(0); }

        new Thread(() -> execInProcess(tc, mod, labelF, tid)).start();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 200); r.put("msg", "ok"); r.put("taskId", taskId); r.put("label", lb);
        return r;
    }

    private void execInProcess(String testClass, String module, String lb, String taskId) {
        long t0 = System.currentTimeMillis();

        try {
            ensureClassIndex();

            LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectPackage("cases"));

            if (!testClass.isBlank()) {
                String fqn = classIndex != null ? classIndex.get(testClass) : null;
                if (fqn != null) {
                    builder.selectors(selectClass(fqn));
                }
                // Fallback: also try by package scan with class filter
                builder.filters(org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns(".*\\." + testClass));
            }

            if (!module.isBlank() && !"ALL".equals(module)) {
                builder.filters(org.junit.platform.launcher.TagFilter.includeTags(module));
            }

            LauncherDiscoveryRequest request = builder.build();
            Launcher launcher = LauncherFactory.create();

            SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(summaryListener);

            launcher.registerTestExecutionListeners(new TestExecutionListener() {
                @Override
                public void executionStarted(TestIdentifier ti) {
                    if (ti.isTest()) {
                        synchronized (logBuf) {
                            logBuf.append("  ▶ ").append(ti.getDisplayName()).append('\n');
                        }
                    }
                }
                @Override
                public void executionFinished(TestIdentifier ti, TestExecutionResult result) {
                    if (ti.isTest()) {
                        progress++;
                        String st = result.getStatus().name();
                        String icon = st.equals("SUCCESSFUL") ? "✓" : "✗";
                        synchronized (logBuf) {
                            logBuf.append("  ").append(icon).append(" [").append(st).append("] ")
                                  .append(ti.getDisplayName());
                            result.getThrowable().ifPresent(t ->
                                logBuf.append(" — ").append(t.getMessage()));
                            logBuf.append('\n');
                        }
                    }
                }
            });

            launcher.execute(request);

            TestExecutionSummary summary = summaryListener.getSummary();
            taskResults.put(taskId, summary);

            durMs = System.currentTimeMillis() - t0;

            long total = summary.getTestsFoundCount();
            long succeeded = summary.getTestsSucceededCount();
            long failed = summary.getTestsFailedCount();

            // Write surefire-style XML for the /results endpoint
            writeSurefireXml(summary, lb);

            progressTotal = (int) total;
            log("Tests run: " + total + ", Failures: " + failed + ", Passed: " + succeeded);

            if (total == 0) {
                status = "FAILED";
                msg = "未发现匹配的测试用例";
            } else if (failed > 0) {
                status = "FAILED";
                msg = lb + " — " + failed + "/" + total + " failed";
            } else {
                status = "SUCCESS";
                msg = lb + " — all " + total + " passed";
            }
        } catch (Exception e) {
            durMs = System.currentTimeMillis() - t0;
            status = "FAILED";
            msg = e.getMessage();
            synchronized (logBuf) {
                logBuf.append("\n[ERROR] ").append(e.getMessage()).append('\n');
            }
        }
    }

    private void ensureClassIndex() {
        if (classIndex != null) return;
        Map<String, String> idx = new HashMap<>();
        try {
            LauncherDiscoveryRequest req = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectPackage("cases"))
                    .build();
            Launcher l = LauncherFactory.create();
            TestPlan plan = l.discover(req);
            for (TestIdentifier root : plan.getRoots()) {
                for (TestIdentifier child : plan.getChildren(root)) {
                    if (child.getSource().isPresent()) {
                        String source = child.getSource().get().toString();
                        // source looks like "ClassSource [className=cases.req_folder.FolderApiTest]"
                        int eq = source.indexOf("className=");
                        if (eq >= 0) {
                            String fqn = source.substring(eq + 10);
                            int end = fqn.indexOf(']');
                            if (end > 0) fqn = fqn.substring(0, end);
                            String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
                            idx.put(simple, fqn);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        classIndex = idx;
    }

    private void log(String s) {
        synchronized (logBuf) {
            logBuf.append(s).append('\n');
        }
    }

    private void writeSurefireXml(TestExecutionSummary summary, String label) {
        try {
            Path dir = Paths.get(System.getProperty("user.dir"), "target/surefire-reports");
            Files.createDirectories(dir);

            // Group results by class
            Map<String, List<TestExecutionSummary.Failure>> byClass = new LinkedHashMap<>();
            for (TestExecutionSummary.Failure f : summary.getFailures()) {
                String cn = f.getTestIdentifier().getUniqueId();
                // Extract class name from uniqueId
                String[] parts = cn.split("\\[");
                for (String p : parts) {
                    if (p.startsWith("class:")) {
                        cn = p.substring(6).replace("]", "");
                        break;
                    }
                }
                byClass.computeIfAbsent(cn, k -> new ArrayList<>()).add(f);
            }

            // Count per class from test identifiers
            Map<String, long[]> counts = new LinkedHashMap<>(); // [total, fail]
            Map<String, Double> times = new LinkedHashMap<>();

            // Write a single summary XML
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<testsuite name=\"").append(escapeXml(label)).append("\"");
            xml.append(" tests=\"").append(summary.getTestsFoundCount()).append("\"");
            xml.append(" failures=\"").append(summary.getTestsFailedCount()).append("\"");
            xml.append(" errors=\"0\"");
            xml.append(" skipped=\"0\"");
            xml.append(" time=\"").append(String.format("%.3f", (double) durMs / 1000)).append("\">\n");

            for (TestExecutionSummary.Failure f : summary.getFailures()) {
                TestIdentifier ti = f.getTestIdentifier();
                xml.append("  <testcase name=\"").append(escapeXml(ti.getDisplayName())).append("\"");
                xml.append(" classname=\"").append(escapeXml(ti.getUniqueId())).append("\"");
                xml.append(" time=\"0\">\n");
                xml.append("    <failure message=\"").append(escapeXml(f.getException().getMessage())).append("\">\n");
                StringWriter sw = new StringWriter();
                f.getException().printStackTrace(new PrintWriter(sw));
                xml.append("<![CDATA[").append(sw.toString()).append("]]>\n");
                xml.append("    </failure>\n");
                xml.append("  </testcase>\n");
            }

            xml.append("</testsuite>");
            Files.writeString(dir.resolve("TEST-" + label.replaceAll("[^a-zA-Z0-9]", "_") + ".xml"),
                    xml.toString());
        } catch (Exception ignored) {}
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000, m = s / 60; s %= 60;
        return m > 0 ? m + "m" + s + "s" : s + "s";
    }

    // ===== Stub endpoints =====

    @GetMapping("/history")
    public List<Map<String, Object>> getHistory() { return Collections.emptyList(); }

    @GetMapping("/history/{taskId}/cases")
    public List<Map<String, Object>> getTaskCases(@PathVariable String taskId) { return Collections.emptyList(); }

    @GetMapping("/failed-cases")
    public List<Map<String, Object>> getFailedCases() { return Collections.emptyList(); }

    @PostMapping("/rerun-failed")
    public Map<String, Object> rerunFailed(@RequestBody Map<String, String> body) {
        return runTests(Map.of("module", "ALL"));
    }

    @DeleteMapping("/history/{taskId}")
    public Map<String, Object> deleteHistory(@PathVariable String taskId) {
        taskResults.remove(taskId);
        return Map.of("code", 200, "msg", "ok");
    }

    @PostMapping("/stop/{taskId}")
    public Map<String, Object> stopTask(@PathVariable String taskId) {
        status = "IDLE"; msg = "手动停止";
        return Map.of("code", 200, "msg", "已停止");
    }

    @GetMapping("/configs")
    public List<Map<String, Object>> getConfigs() { return Collections.emptyList(); }

    @PostMapping("/configs")
    public Map<String, Object> saveConfig(@RequestBody Map<String, String> body) {
        return Map.of("code", 200, "msg", "ok", "id", 1);
    }

    @DeleteMapping("/configs/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        return Map.of("code", 200, "msg", "ok");
    }
}
