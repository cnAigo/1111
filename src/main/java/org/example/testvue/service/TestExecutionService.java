package org.example.testvue.service;

import com.google.gson.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
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
    // Uses Playwright APIRequestContext to load auth.json (browser SSO state),
    // then calls target server APIs directly. Playwright is launched with
    // PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 to avoid browser driver download.

    public synchronized void startCleanup(String projectId, String url, String username, String password) {
        if ("RUNNING".equals(status)) return;
        stopFlag.set(false);
        status = "RUNNING"; msg = ""; label = "清理环境";
        durMs = 0; progress = 0; progressTotal = 0;
        synchronized (logBuf) { logBuf.setLength(0); }
        currentTaskId = "cleanup";
        String base = (url != null && !url.isBlank()) ? url : "https://192.168.6.171:8088";
        String user = (username != null && !username.isBlank()) ? username : "admin";
        String pass = (password != null && !password.isBlank()) ? password : "Aa123456";
        if ((projectId == null || projectId.isBlank())) {
            List<TestConfigEntity> cfs = configRepo.findAll();
            if (!cfs.isEmpty()) {
                TestConfigEntity c = cfs.get(0);
                if (c.getProjectId() != null && !c.getProjectId().isBlank()) projectId = c.getProjectId();
            }
        }
        String fBase = base, fUser = user, fPass = pass, fProjectId = projectId;
        new Thread(() -> execCleanup(fBase, fUser, fPass, fProjectId)).start();
    }

    private void execCleanup(String base, String user, String pass, String projectId) {
        long t0 = System.currentTimeMillis();
        Playwright playwright = null;
        Browser browser = null;
        try {
            log("====== 开始清理环境 ======");
            if (projectId == null || projectId.isBlank()) { status = "FAILED"; msg = "未配置projectId"; return; }
            String apiBase = base + "/dev-api";
            Path authPath = Paths.get("auth.json");

            // Launch browser
            log("启动浏览器...");
            playwright = Playwright.create();
            BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);
            String chromeEnv = System.getenv("CHROME_PATH");
            if (chromeEnv != null && !chromeEnv.isBlank() && Files.exists(Paths.get(chromeEnv))) {
                launchOpts.setExecutablePath(Paths.get(chromeEnv));
            } else {
                Path defChrome = Paths.get("C:/Program Files/Google/Chrome/Application/chrome.exe");
                if (Files.exists(defChrome)) launchOpts.setExecutablePath(defChrome);
            }
            browser = playwright.chromium().launch(launchOpts);

            // Step 1: Create context WITH auth.json if exists, test if still valid
            BrowserContext ctx;
            Page page;
            Browser.NewContextOptions ctxOpts = new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true).setViewportSize(1920, 1080);
            boolean hasAuth = Files.exists(authPath);
            if (hasAuth) {
                ctxOpts.setStorageStatePath(authPath);
                log("已加载 auth.json");
            }
            ctx = browser.newContext(ctxOpts);
            page = ctx.newPage();
            page.setDefaultTimeout(15000);

            APIRequestContext api = page.request();
            boolean authOk = false;

            if (hasAuth) {
                log("验证认证状态...");
                try {
                    // Refresh authCode via API
                    api.post(base + "/login-api/auth/token/login",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"));
                    // Test tree API
                    APIResponse test = api.post(apiBase + "/erm/search/searchReqFolderStructureTree",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"projectId\":\"" + projectId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
                    if (test.status() == 200) {
                        authOk = true;
                        log("认证有效");
                    } else {
                        log("认证已过期(HTTP " + test.status() + ")，需要重新登录");
                    }
                } catch (Exception e) {
                    log("认证检查异常: " + e.getMessage());
                }
            }

            if (!authOk) {
                // Close old context, create fresh one WITHOUT auth.json
                ctx.close();
                try { Files.deleteIfExists(authPath); } catch (Exception ignored) {}
                log("创建全新浏览器会话...");
                ctx = browser.newContext(new Browser.NewContextOptions()
                    .setIgnoreHTTPSErrors(true).setViewportSize(1920, 1080));
                page = ctx.newPage();
                page.setDefaultTimeout(15000);
                api = page.request();

                // Navigate to app, wait for SPA to redirect to login
                log("导航到目标系统...");
                page.navigate(base + "/#/RequirementManagement");
                try { page.waitForLoadState(); } catch (Exception ignored) {}
                Thread.sleep(5000);

                String currentUrl = page.url();
                log("当前URL: " + currentUrl);

                // Check if on login page
                boolean onLoginPage = currentUrl.contains("/login")
                    || page.locator("input[type='password']").count() > 0;

                if (onLoginPage) {
                    log("检测到登录表单，自动登录...");
                    try {
                        page.locator("input[type='text']").first().fill(user);
                        page.locator("input[type='password']").first().fill(pass);
                        page.locator("button[type='submit']").first().click();
                        page.waitForURL(url -> !url.contains("/login"), new Page.WaitForURLOptions().setTimeout(30000));
                        log("SSO 登录完成");
                    } catch (Exception e) {
                        log("登录交互失败: " + e.getMessage());
                    }
                } else {
                    // Maybe SPA uses hash routing; try navigating to login page directly
                    log("未检测到登录页，尝试直接访问登录页...");
                    try {
                        page.navigate(base + "/login");
                        Thread.sleep(3000);
                        if (page.locator("input[type='password']").count() > 0) {
                            page.locator("input[type='text']").first().fill(user);
                            page.locator("input[type='password']").first().fill(pass);
                            page.locator("button[type='submit']").first().click();
                            page.waitForURL(url -> !url.contains("/login"), new Page.WaitForURLOptions().setTimeout(30000));
                            log("登录完成(直接访问/login)");
                        }
                    } catch (Exception e2) {
                        log("直接访问登录页也失败: " + e2.getMessage());
                    }
                }

                // Save new auth state
                try { ctx.storageState(new BrowserContext.StorageStateOptions().setPath(authPath)); } catch (Exception ignored) {}
                log("已保存新认证到 auth.json");

                // Refresh api context after login
                api = page.request();
                api.post(base + "/login-api/auth/token/login",
                    RequestOptions.create().setHeader("Content-Type", "application/json")
                        .setData("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"));
                try { ctx.storageState(new BrowserContext.StorageStateOptions().setPath(authPath)); } catch (Exception ignored) {}
            }

            // Get tree
            log("获取目录树...");
            APIResponse treeResp = api.post(apiBase + "/erm/search/searchReqFolderStructureTree",
                RequestOptions.create().setHeader("Content-Type", "application/json")
                    .setData("{\"projectId\":\"" + projectId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
            String treeBody = treeResp.text();
            if (treeResp.status() != 200) {
                log("树API返回 " + treeResp.status() + ": " + treeBody.substring(0, Math.min(300, treeBody.length())));
                status = "FAILED"; msg = "认证失败，请先在浏览器中手动登录一次目标系统"; return;
            }
            JsonObject root = new Gson().fromJson(treeBody, JsonObject.class);
            if (!root.has("data") || root.get("data").isJsonNull()) {
                log("树响应无data: " + treeBody.substring(0, Math.min(200, treeBody.length())));
                status = "SUCCESS"; msg = "空"; return;
            }
            JsonArray dataList = root.getAsJsonArray("data");
            if (dataList.size() == 0) { status = "SUCCESS"; msg = "空"; return; }

            // Count and clean
            List<JsonObject> workList = new ArrayList<>();
            for (JsonElement el : dataList) {
                JsonObject node = el.getAsJsonObject();
                workList.add(node);
                countChildren(node, workList);
            }
            int total = workList.size();
            progressTotal = total; progress = 0;
            log("找到 " + total + " 个节点，开始清理...");

            int count = 0;
            for (JsonElement el : dataList) {
                if (stopFlag.get()) { status = "STOPPED"; return; }
                JsonObject node = el.getAsJsonObject();
                String nodeId = node.get("objectId").getAsString();
                String nodeTitle = node.has("title") ? node.get("title").getAsString() : nodeId;
                String nodeType = node.get("type").getAsString();

                if (node.has("children") && !node.get("children").isJsonNull()) {
                    for (JsonElement childEl : node.getAsJsonArray("children")) {
                        if (stopFlag.get()) { status = "STOPPED"; return; }
                        JsonObject child = childEl.getAsJsonObject();
                        String cId = child.get("objectId").getAsString();
                        String cTitle = child.has("title") ? child.get("title").getAsString() : cId;
                        String cType = child.get("type").getAsString();

                        if ("reqSpeFolder".equals(cType)) {
                            api.post(apiBase + "/erm/del/delReqSpeFolder",
                                RequestOptions.create().setHeader("Content-Type", "application/json")
                                    .setData("{\"objectId\":\"" + cId + "\",\"parentId\":\"" + nodeId + "\",\"parentType\":\"reqSpeFolder\"}"));
                            Thread.sleep(300);
                            api.post(apiBase + "/erm/clean/cleanReqSpeFolder",
                                RequestOptions.create().setHeader("Content-Type", "application/json")
                                    .setData("{\"objectId\":\"" + cId + "\",\"parentId\":\"" + nodeId + "\",\"parentType\":\"reqSpeFolder\"}"));
                            Thread.sleep(200);
                            count++; progress = count;
                            log("[CLEANUP] " + count + "/" + total + " 子文件夹 " + cTitle);
                        } else if ("reqSpe".equals(cType)) {
                            api.post(apiBase + "/erm/del/delReqSpe",
                                RequestOptions.create().setHeader("Content-Type", "application/json")
                                    .setData("{\"objectId\":\"" + cId + "\",\"parentId\":\"" + nodeId + "\",\"parentType\":\"reqSpeFolder\"}"));
                            Thread.sleep(300);
                            api.post(apiBase + "/erm/clean/cleanReqSpe",
                                RequestOptions.create().setHeader("Content-Type", "application/json")
                                    .setData("{\"objectId\":\"" + cId + "\",\"parentId\":\"" + nodeId + "\",\"parentType\":\"reqSpeFolder\"}"));
                            Thread.sleep(200);
                            count++; progress = count;
                            log("[CLEANUP] " + count + "/" + total + " 需求规格 " + cTitle);
                        }
                    }
                }

                if ("reqSpeFolder".equals(nodeType)) {
                    api.post(apiBase + "/erm/del/delReqSpeFolder",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
                    Thread.sleep(300);
                    api.post(apiBase + "/erm/clean/cleanReqSpeFolder",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
                    Thread.sleep(200);
                    count++; progress = count;
                    log("[CLEANUP] " + count + "/" + total + " 文件夹 " + nodeTitle);
                } else if ("reqSpe".equals(nodeType)) {
                    api.post(apiBase + "/erm/del/delReqSpe",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
                    Thread.sleep(300);
                    api.post(apiBase + "/erm/clean/cleanReqSpe",
                        RequestOptions.create().setHeader("Content-Type", "application/json")
                            .setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}"));
                    Thread.sleep(200);
                    count++; progress = count;
                    log("[CLEANUP] " + count + "/" + total + " 需求规格 " + nodeTitle);
                }
            }

            durMs = System.currentTimeMillis() - t0;
            status = "SUCCESS"; msg = "清理完成，共 " + count + " 个节点";
            log("====== 清理完成！共 " + count + " 个节点 ======");
        } catch (Exception e) {
            durMs = System.currentTimeMillis() - t0;
            status = "FAILED"; msg = e.getMessage();
            log("ERROR: " + e.getMessage());
        } finally {
            if (playwright != null) { try { playwright.close(); } catch (Exception ignored) {} }
        }
    }

    private void countChildren(JsonObject node, List<JsonObject> acc) {
        if (node.has("children") && !node.get("children").isJsonNull()) {
            for (JsonElement c : node.getAsJsonArray("children")) {
                JsonObject child = c.getAsJsonObject();
                acc.add(child);
                countChildren(child, acc);
            }
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
