package org.example.testvue.controller;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/test")
public class TestRunnerController {

    private static final long TEST_TO_MIN = 15;
    private static final long RPT_TO_MIN = 3;
    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private volatile String status = "READY";
    private volatile String msg = "";
    private volatile String label = "";
    private volatile long durMs = 0;
    private volatile int progress = 0;
    private volatile int progressTotal = 0;

    private final StringBuilder logBuf = new StringBuilder();

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("msg", msg);
        m.put("label", label);
        m.put("durationMs", durMs);
        m.put("durationFmt", fmt(durMs));
        m.put("progress", progress);
        m.put("progressTotal", progressTotal);
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
            String raw = Files.readString(p, java.nio.charset.StandardCharsets.UTF_8);
            // Parse with simple string ops to avoid Gson dependency issues
            m.put("raw", raw);
            m.put("total",   extractInt(raw, "total"));
            m.put("passed",  extractInt(raw, "passed"));
            int f = extractInt(raw, "failed");
            int b = extractInt(raw, "broken");
            m.put("failed",  f + b);
            m.put("skipped", extractInt(raw, "skipped"));
            // sumDuration is in ms, convert to seconds for display
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
        List<Map<String, Object>> list = new ArrayList<>();
        Path dir = Paths.get(System.getProperty("user.dir"), "target/surefire-reports");
        if (!Files.exists(dir)) return list;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "TEST-*.xml")) {
            for (Path f : ds) {
                String xml = Files.readString(f, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, Object> cls = new LinkedHashMap<>();
                cls.put("className", shortName(attr(xml, "testsuite", "name")));
                cls.put("tests",   Integer.parseInt(attr(xml, "testsuite", "tests")));
                cls.put("failures",Integer.parseInt(attr(xml, "testsuite", "failures")));
                cls.put("errors",  Integer.parseInt(attr(xml, "testsuite", "errors")));
                cls.put("skipped", Integer.parseInt(attr(xml, "testsuite", "skipped")));
                cls.put("time",    attr(xml, "testsuite", "time"));

                List<Map<String, String>> cases = new ArrayList<>();
                int idx = 0;
                while ((idx = xml.indexOf("<testcase ", idx)) >= 0) {
                    int tagEnd = xml.indexOf(">", idx);
                    // Find the matching </testcase>
                    int closeIdx = xml.indexOf("</testcase>", tagEnd);
                    if (closeIdx < 0) closeIdx = xml.indexOf("/>", tagEnd);
                    String full = closeIdx > tagEnd ? xml.substring(idx, closeIdx) : xml.substring(idx, tagEnd + 1);
                    String name = attr(full, "testcase", "name");
                    String tm = attr(full, "testcase", "time");
                    boolean fail = full.contains("<failure");
                    boolean err  = full.contains("<error") && !full.contains("<error ");

                    // Extract failure reason with full CDATA content
                    String reason = "";
                    if (fail || err) {
                        String ftag = fail ? "failure" : "error";
                        int fi = full.indexOf("<" + ftag);
                        if (fi >= 0) {
                            // Get message attribute
                            reason = attr(full.substring(fi), ftag, "message");
                            // Unescape
                            reason = reason.replace("&lt;", "<").replace("&gt;", ">")
                                           .replace("&amp;", "&").replace("&quot;", "\"")
                                           .replace("&apos;", "'");
                            // Extract CDATA for full stack trace
                            int cd = full.indexOf("<![CDATA[", fi);
                            if (cd >= 0) {
                                int ce = full.indexOf("]]>", cd);
                                if (ce > cd) {
                                    String cdata = full.substring(cd + 9, ce).trim();
                                    // Take first 4 meaningful lines
                                    String[] lines = cdata.split("\\r?\\n");
                                    StringBuilder sb = new StringBuilder();
                                    int taken = 0;
                                    for (String ln : lines) {
                                        String t = ln.trim();
                                        if (t.isEmpty()) continue;
                                        sb.append(t).append("\n");
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
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", e.getMessage());
            list.add(err);
        }
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

    @PostMapping("/run")
    public Map<String, Object> runTests(@RequestBody Map<String, String> req) {
        String testClass = req.getOrDefault("testClass", "");
        String module = req.getOrDefault("module", "");

        String lb;
        if (!testClass.isBlank()) lb = testClass;
        else if (!module.isBlank() && !"ALL".equals(module)) lb = module;
        else lb = "ALL";

        final String tc = testClass, mod = module, labelF = lb;
        synchronized (logBuf) { logBuf.setLength(0); }

        new Thread(() -> execBg(tc, mod, labelF)).start();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", 200); r.put("msg", "ok"); r.put("label", lb);
        return r;
    }

    private void execBg(String testClass, String module, String lb) {
        status = "RUNNING"; msg = ""; label = lb; durMs = 0; progress = 0;
        progressTotal = testClass.isBlank() ? 10 : 1;
        long t0 = System.currentTimeMillis();

        try {
            List<String> cmd = mvnCmd();
            cmd.add("surefire:test");
            if (!testClass.isBlank()) cmd.add("-Dtest=" + testClass);
            else if (!module.isBlank() && !"ALL".equals(module)) cmd.add("-Dgroups=" + module);

            int code = run(cmd, TEST_TO_MIN, TimeUnit.MINUTES);

            List<String> ac = mvnCmd();
            ac.add("allure:report");
            run(ac, RPT_TO_MIN, TimeUnit.MINUTES);

            durMs = System.currentTimeMillis() - t0;
            status = (code == 0) ? "SUCCESS" : "FAILED";
            msg = status.equals("SUCCESS") ? lb + " passed" : lb + " exit=" + code;
        } catch (Exception e) {
            durMs = System.currentTimeMillis() - t0;
            status = "FAILED"; msg = e.getMessage();
            synchronized (logBuf) { logBuf.append("\n[ERROR] ").append(e.getMessage()).append('\n'); }
        }
    }

    private List<String> mvnCmd() {
        List<String> c = new ArrayList<>();
        if (IS_WIN) { c.add("cmd"); c.add("/c"); c.add("mvnw.cmd"); }
        else { c.add("./mvnw"); }
        return c;
    }

    private int run(List<String> cmd, long to, TimeUnit u) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        Charset cs = IS_WIN ? Charset.forName("GBK") : Charset.defaultCharset();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), cs))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(line);
                if (line.contains("Tests run:")) progress++;
                synchronized (logBuf) {
                    logBuf.append(line).append('\n');
                    if (logBuf.length() > 50_000) logBuf.delete(0, logBuf.length() - 40_000);
                }
            }
        }

        if (!p.waitFor(to, u)) { p.destroyForcibly(); return -1; }
        return p.exitValue();
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000, m = s / 60; s %= 60;
        return m > 0 ? m + "m" + s + "s" : s + "s";
    }
}
