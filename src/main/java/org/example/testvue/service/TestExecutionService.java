package org.example.testvue.service;

import com.google.gson.*;
import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestConfigEntity;
import org.example.testvue.entity.TestHistory;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestConfigRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TestExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TestExecutionService.class);
    private static final boolean IS_WIN = System.getProperty("os.name").toLowerCase().contains("win");

    private final TestHistoryRepository historyRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final TestConfigRepository configRepo;

    private volatile String status = "IDLE";
    private volatile String msg = "";
    private volatile String label = "";
    private volatile long durMs = 0;
    private volatile int progress = 0;
    private volatile int progressTotal = 0;
    private volatile String currentTaskId;
    private final StringBuilder logBuf = new StringBuilder();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private final Object processLock = new Object();
    private Process runningProcess;

    private static final HttpClient HC;
    static {
        try {
            TrustManager[] a = {new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String s) {}
                public void checkServerTrusted(X509Certificate[] c, String s) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, a, new java.security.SecureRandom());
            javax.net.ssl.SSLParameters params = new javax.net.ssl.SSLParameters();
            params.setEndpointIdentificationAlgorithm("");
            HC = HttpClient.newBuilder().sslContext(ssl).sslParameters(params)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public TestExecutionService(TestHistoryRepository historyRepo, TestCaseDetailRepository caseDetailRepo,
                                 TestConfigRepository configRepo) {
        this.historyRepo = historyRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.configRepo = configRepo;
    }

    public synchronized String startRun(TestRunRequest req) {
        if ("RUNNING".equals(status)) return null;
        stopFlag.set(false);
        String tid = UUID.randomUUID().toString();
        String lb;
        if (req.testClass != null && !req.testClass.isBlank()) lb = req.testClass;
        else if (req.module != null && !req.module.isBlank() && !"ALL".equals(req.module)) lb = req.module;
        else lb = "ALL";

        status = "RUNNING"; msg = ""; label = lb; durMs = 0;
        progress = 0; currentTaskId = tid;
        synchronized (logBuf) { logBuf.setLength(0); }

        // Count test cases from DB; fall back to mvn output if DB is empty
        if (req.testClass != null && !req.testClass.isBlank()) {
            String[] classes = req.testClass.split(",");
            long count = 0;
            for (String cls : classes) count += caseDetailRepo.findByClassName(cls.trim()).size();
            progressTotal = (int) count;
        } else {
            progressTotal = (int) caseDetailRepo.count();
        }
        if (progressTotal <= 0) progressTotal = 1; // prevent 0% display

        new Thread(() -> execMvn(tid, req, lb)).start();
        return tid;
    }

    public void stopRun() {
        stopFlag.set(true);
        synchronized (processLock) {
            if (runningProcess != null && runningProcess.isAlive()) runningProcess.destroyForcibly();
        }
        status = "IDLE"; msg = "手动停止";
    }

    public StatusResponse getCurrentStatus() {
        StatusResponse s = new StatusResponse();
        s.status = status; s.msg = msg; s.label = label;
        s.durationMs = durMs; s.durationFmt = fmt(durMs);
        s.progress = progress; s.progressTotal = progressTotal;
        s.errorMessage = msg; s.taskId = currentTaskId;
        synchronized (logBuf) { s.output = logBuf.toString(); }
        return s;
    }

    public boolean isRunning() { return "RUNNING".equals(status); }

    // ── Cleanup ──

    public synchronized void startCleanup(String projectId, String url, String username, String password) {
        if ("RUNNING".equals(status)) return;
        stopFlag.set(false);
        status = "RUNNING"; msg = ""; label = "清理环境";
        durMs = 0; progress = 0; progressTotal = 100;
        synchronized (logBuf) { logBuf.setLength(0); }
        String base = (url != null && !url.isBlank()) ? url : "https://192.168.6.171:8088";
        String user = (username != null && !username.isBlank()) ? username : "admin";
        String pass = (password != null && !password.isBlank()) ? password : "Aa123456";
        // Fallback to DB config
        if ((projectId == null || projectId.isBlank()) && (base == null || base.isBlank())) {
            List<TestConfigEntity> cfs = configRepo.findAll();
            if (!cfs.isEmpty()) {
                TestConfigEntity c = cfs.get(0);
                if (c.getUrl() != null && !c.getUrl().isBlank()) base = c.getUrl();
                if (c.getUsername() != null && !c.getUsername().isBlank()) user = c.getUsername();
                if (c.getPassword() != null && !c.getPassword().isBlank()) pass = c.getPassword();
                if (c.getProjectId() != null && !c.getProjectId().isBlank()) projectId = c.getProjectId();
            }
        }
        String fBase = base, fUser = user, fPass = pass, fProjectId = projectId;
        new Thread(() -> execCleanup(fBase, fUser, fPass, fProjectId)).start();
    }

    private void execCleanup(String base, String user, String pass, String projectId) {
        long t0 = System.currentTimeMillis();
        try {
            log("====== 开始清理环境 ======");
            if (projectId == null || projectId.isBlank()) { status = "FAILED"; msg = "未配置projectId"; return; }
            String apiBase = base + "/dev-api";

            // Login
            log("正在登录...");
            String loginBody = "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}";
            HttpRequest loginReq = HttpRequest.newBuilder().uri(URI.create(base + "/login-api/auth/token/login"))
                .timeout(java.time.Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody)).build();
            HttpResponse<String> loginResp = HC.send(loginReq, HttpResponse.BodyHandlers.ofString());
            String cookie = loginResp.headers().firstValue("Set-Cookie").orElse("");
            log("登录完成");

            // Get tree
            log("获取目录树...");
            HttpRequest treeReq = HttpRequest.newBuilder().uri(URI.create(apiBase + "/erm/search/searchReqFolderStructureTree"))
                .timeout(java.time.Duration.ofSeconds(30)).header("Content-Type", "application/json").header("Cookie", cookie)
                .POST(HttpRequest.BodyPublishers.ofString("{\"projectId\":\"" + projectId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}")).build();
            String treeResp = HC.send(treeReq, HttpResponse.BodyHandlers.ofString()).body();
            JsonObject treeRoot = new Gson().fromJson(treeResp, JsonObject.class);
            if (treeRoot.get("code").getAsInt() != 200) { status = "FAILED"; msg = "获取目录树失败"; return; }
            JsonArray treeData = treeRoot.getAsJsonArray("data");
            if (treeData == null || treeData.size() == 0) { log("空树，无需清理"); status = "SUCCESS"; msg = "目录树为空"; return; }

            // Collect nodes
            List<String[]> nodes = new ArrayList<>();
            collectCleanupNodes(treeData, nodes);
            Collections.reverse(nodes);
            log("找到 " + nodes.size() + " 个节点");
            progressTotal = nodes.size(); progress = 0;

            int count = 0;
            for (String[] node : nodes) {
                if (stopFlag.get()) { status = "IDLE"; msg = "已停止"; return; }
                String type = node[0], objectId = node[1], parentId = node[2];
                String pt = projectId.equals(parentId) ? "project" : "reqSpeFolder";

                String delPath = "reqSpe".equals(type) ? "/erm/del/delReqSpe" : "req".equals(type) ? "/erm/del/delReqObjectList" : "/erm/del/delReqSpeFolder";
                String delBody = "req".equals(type) ? "{\"objectId\":\"" + objectId + "\"}" : "{\"objectId\":\"" + objectId + "\",\"parentId\":\"" + parentId + "\",\"parentType\":\"" + pt + "\"}";
                sendCleanup(apiBase + delPath, cookie, delBody);
                Thread.sleep(500);

                String cleanPath = "reqSpe".equals(type) ? "/erm/clean/cleanReqSpe" : "req".equals(type) ? "/erm/clean/cleanReq" : "/erm/clean/cleanReqSpeFolder";
                String cleanBody = "req".equals(type) ? "{\"objectId\":\"" + objectId + "\",\"reqSpecId\":\"" + parentId + "\"}" : "{\"objectId\":\"" + objectId + "\",\"parentId\":\"" + parentId + "\",\"parentType\":\"" + pt + "\"}";
                sendCleanup(apiBase + cleanPath, cookie, cleanBody);
                Thread.sleep(500);

                count++; progress = count;
                if (count % 5 == 0) log("已清理 " + count + "/" + nodes.size());
            }
            durMs = System.currentTimeMillis() - t0;
            status = "SUCCESS"; msg = "清理完成，共 " + count + " 个节点";
            log("====== 清理完成！共 " + count + " 个节点 ======");
        } catch (Exception e) {
            durMs = System.currentTimeMillis() - t0;
            status = "FAILED"; msg = e.getMessage();
            log("ERROR: " + e.getMessage());
        }
    }

    private void sendCleanup(String url, String cookie, String body) throws Exception {
        HC.send(HttpRequest.newBuilder().uri(URI.create(url)).timeout(java.time.Duration.ofSeconds(30))
            .header("Content-Type", "application/json").header("Cookie", cookie)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void collectCleanupNodes(JsonArray items, List<String[]> nodes) {
        for (JsonElement el : items) {
            JsonObject node = el.getAsJsonObject();
            String type = node.has("type") ? node.get("type").getAsString() : "";
            String id = node.has("objectId") ? node.get("objectId").getAsString() : "";
            String pid = node.has("parentId") ? node.get("parentId").getAsString() : "";
            if (node.has("children") && !node.get("children").isJsonNull())
                collectCleanupNodes(node.getAsJsonArray("children"), nodes);
            if (!id.isBlank() && !"project".equals(type))
                nodes.add(new String[]{type, id, pid});
        }
    }

    // ── Private ──

    private void execMvn(String tid, TestRunRequest req, String lb) {
        long t0 = System.currentTimeMillis();
        String mvn = IS_WIN ? "mvnw.cmd" : "./mvnw";
        try {
            Path sfDir = Paths.get(System.getProperty("user.dir"), "target/surefire-reports");
            try { if (Files.exists(sfDir)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(sfDir, "TEST-*.xml")) {
                    for (Path f : ds) Files.deleteIfExists(f);
                }
            }} catch (Exception ignored) {}

            List<String> cmd = new ArrayList<>();
            cmd.add(mvn); cmd.add("test");
            if (req.testClass != null && !req.testClass.isBlank()) {
                cmd.add("-Dtest=" + req.testClass);
            } else if (req.module != null && !req.module.isBlank() && !"ALL".equals(req.module)) {
                cmd.add("-Dgroups=" + req.module);
            }
            cmd.add("-DfailIfNoTests=false");
            cmd.add("-Dplaywright.headless=true");

            log("▶ " + String.join(" ", cmd.subList(1, cmd.size())));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            // Pass frontend config to subprocess via env vars (TestConfig reads them)
            if (req.url != null && !req.url.isBlank()) pb.environment().put("BASE_URL", req.url);
            if (req.username != null && !req.username.isBlank()) pb.environment().put("TAAS_USER", req.username);
            if (req.password != null && !req.password.isBlank()) pb.environment().put("TAAS_PASS", req.password);
            if (req.projectId != null && !req.projectId.isBlank()) pb.environment().put("TAAS_PROJECT_ID", req.projectId);
            synchronized (processLock) { runningProcess = pb.start(); }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(runningProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (stopFlag.get()) { runningProcess.destroyForcibly(); break; }
                    synchronized (logBuf) { logBuf.append(line).append('\n'); }
                    if (line.startsWith("Tests run:")) {
                        try { progress = extractInt(line, "Tests run:"); } catch (Exception ignored) {}
                    }
                }
            }
            int exitCode = runningProcess.waitFor();
            synchronized (processLock) { runningProcess = null; }
            if (stopFlag.get()) { status = "STOPPED"; return; }

            durMs = System.currentTimeMillis() - t0;
            List<ClassResult> results = SurefireParser.parseDir(sfDir);
            int total = 0, failed = 0, passed = 0;
            for (ClassResult r : results) {
                total += r.tests;
                failed += r.failures + r.errors;
                passed += r.tests - r.failures - r.errors - r.skipped;
            }
            progressTotal = total; progress = total;

            // Generate Allure
            try {
                List<String> ac = new ArrayList<>(); ac.add(mvn); ac.add("allure:report"); ac.add("-q");
                new ProcessBuilder(ac).directory(new File(System.getProperty("user.dir"))).start().waitFor(30, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            String resultJson = new com.google.gson.Gson().toJson(results);
            log("Tests run: " + total + ", Failures: " + failed + ", Passed: " + passed);

            if (total == 0 && exitCode != 0) { status = "FAILED"; msg = "mvn test 失败 (exit=" + exitCode + ")"; }
            else if (total == 0) { status = "FAILED"; msg = "未发现匹配的测试用例"; }
            else if (failed > 0) { status = "FAILED"; msg = lb + " — " + failed + "/" + total + " failed"; }
            else { status = "SUCCESS"; msg = lb + " — all " + total + " passed"; }

            try {
                String out; synchronized (logBuf) { out = logBuf.toString(); }
                historyRepo.save(TestHistory.of(tid, lb, status, fmt(durMs), passed, failed, 0, out, resultJson));
            } catch (Exception e) { LOG.warn("Failed to persist history: {}", e.getMessage()); }

        } catch (Exception e) {
            durMs = System.currentTimeMillis() - t0;
            status = "FAILED"; msg = e.getMessage();
            synchronized (logBuf) { logBuf.append("\n[ERROR] ").append(e.getMessage()).append('\n'); }
        }
    }

    private void log(String s) { synchronized (logBuf) { logBuf.append(s).append('\n'); } }

    private static int extractInt(String s, String prefix) {
        int i = s.indexOf(prefix); if (i < 0) return 0;
        String sub = s.substring(i + prefix.length()).trim();
        int end = 0;
        while (end < sub.length() && Character.isDigit(sub.charAt(end))) end++;
        return Integer.parseInt(sub.substring(0, end));
    }

    private static String fmt(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000, m = s / 60; s %= 60;
        return m > 0 ? m + "m" + s + "s" : s + "s";
    }
}
