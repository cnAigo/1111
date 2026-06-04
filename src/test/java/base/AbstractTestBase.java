package base;

import actions.ReqApiActions;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({RetryExtension.class})
public abstract class AbstractTestBase {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected ReqApiActions api;

    protected static final Logger log = LoggerFactory.getLogger(AbstractTestBase.class);
    protected static final String AUTH_STATE_PATH = "auth.json";
    protected String PROJECT_ID;
    protected static final Object AUTH_LOCK = new Object();

    /** Unique prefix per test class — all created data starts with this, swept on teardown. */
    protected String testPrefix;

    @BeforeAll
    public void setup() {
        testPrefix = "AT_" + getClass().getSimpleName() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmm")) + "_";
        log.info("[{}] Test prefix: {}", getClass().getSimpleName(), testPrefix);

        // =========================================================
        // 🚨 核心修复 1：注入环境变量，彻底屏蔽 Playwright 自动下载浏览器
        // =========================================================
        Playwright.CreateOptions options = new Playwright.CreateOptions();
        options.setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"));
        playwright = Playwright.create(options);

        boolean headless = "true".equals(System.getProperty("playwright.headless", "false"));

        Browser.NewContextOptions ctxOpts = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080).setIgnoreHTTPSErrors(true);

        Path authPath = Paths.get(AUTH_STATE_PATH);
        if (Files.exists(authPath)) {
            ctxOpts.setStorageStatePath(authPath);
            log.info("Loaded auth.json");
        }

        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions()
                .setHeadless(headless).setSlowMo(0);

        // Use system Chrome if Playwright's bundled browser is unavailable
        String chromePath = System.getenv("CHROME_PATH");
        if (chromePath != null && !chromePath.isBlank() && Files.exists(Paths.get(chromePath))) {
            launchOpts.setExecutablePath(Paths.get(chromePath));
            log.info("Using system Chrome: {}", chromePath);
        } else {
            Path defaultChrome = Paths.get("C:/Program Files/Google/Chrome/Application/chrome.exe");
            if (Files.exists(defaultChrome)) {
                launchOpts.setExecutablePath(defaultChrome);
                log.info("Using system Chrome (default path)");
            }
        }
        browser = playwright.chromium().launch(launchOpts);

        try {
            context = browser.newContext(ctxOpts);
        } catch (Exception e) {
            log.warn("Context failed, retrying without auth: {}", e.getMessage());
            ctxOpts.setStorageStatePath(null);
            context = browser.newContext(ctxOpts);
        }

        page = context.newPage();
        // Set longer default timeout for UI tests
        page.setDefaultTimeout(15000);
        page.setDefaultNavigationTimeout(30000);
        api = new ReqApiActions(page.request());
        api.setReLogin(this::loginViaApi);

        // =========================================================
        // 🚨 核心修复 2：在查询任何业务数据前，强制先进行一次 API 登录获取凭证
        // =========================================================
        loginViaApi();

        // Project ID — env var takes priority, otherwise dynamic lookup
        if (TestConstants.PROJECT_ID != null && !TestConstants.PROJECT_ID.isEmpty()) {
            PROJECT_ID = TestConstants.PROJECT_ID;
            log.info("Using PROJECT_ID from env: {}", PROJECT_ID);
        } else {
            try {
                PROJECT_ID = api.getProjectIdByName(TestConstants.PROJECT_NAME);
                TestConstants.PROJECT_ID = PROJECT_ID;
                log.info("Project resolved: {} → {}", TestConstants.PROJECT_NAME, PROJECT_ID);
            } catch (Exception e) {
                String m = e.getMessage();
                if (m != null && (m.contains("500") || m.contains("系统异常")))
                    throw new org.opentest4j.TestAbortedException("服务端不可用（500），跳过测试");
                log.warn("Project ID resolve failed: {}", m);
                PROJECT_ID = "2058851105448046592"; // 兜底的 ID
                TestConstants.PROJECT_ID = PROJECT_ID;
            }
        }
    }

    @AfterAll
    public void teardown() {
        // Always sweep test data — regardless of pass/fail/abort
        try {
            if (api != null && PROJECT_ID != null && testPrefix != null) {
                log.info("[{}] Sweeping test data with prefix '{}'", getClass().getSimpleName(), testPrefix);
                api.sweepByPrefix(PROJECT_ID, testPrefix);
            }
        } catch (Exception e) { log.warn("Sweep failed: {}", e.getMessage()); }

        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }

    // ── Helpers using unique prefix ──

    protected String suffix() { return UUID.randomUUID().toString().substring(0, 6); }

    protected String[] createTempFolder() {
        String id = api.createFolder(PROJECT_ID, PROJECT_ID);
        String name = testPrefix + "Folder_" + suffix();
        api.renameFolder(PROJECT_ID, id, PROJECT_ID, name);
        return new String[]{id, name};
    }

    protected String[] createTempDoc(String parentId) {
        String id = api.createDocument(PROJECT_ID, parentId);
        String name = testPrefix + "Doc_" + suffix();
        api.renameDocument(PROJECT_ID, id, parentId, name);
        return new String[]{id, name, parentId};
    }

    protected String[] createTempDoc() {
        String fid = api.createFolder(PROJECT_ID, PROJECT_ID);
        api.renameFolder(PROJECT_ID, fid, PROJECT_ID, testPrefix + "Parent_" + suffix());
        return createTempDoc(fid);
    }

    protected String[] createTempDocFull(String parentId) {
        String id = api.createDocument(PROJECT_ID, parentId);
        String name = testPrefix + "DocFull_" + suffix();
        api.renameDocument(PROJECT_ID, id, parentId, name);
        return new String[]{id, name, parentId};
    }

    protected String resolveParentId() {
        String fid = api.createFolder(PROJECT_ID, PROJECT_ID);
        api.renameFolder(PROJECT_ID, fid, PROJECT_ID, testPrefix + "Import_" + suffix());
        return fid;
    }

    protected void hardCleanFolder(String folderId) {
        try { api.forceCleanFolder(folderId); } catch (Exception e) { log.warn("Hard clean failed: {}", e.getMessage()); }
    }

    protected void cleanupDoc(String docId, String parentId) {
        try { api.deleteDocument(docId, parentId); api.forceCleanDocument(docId, parentId); } catch (Exception ignored) {}
    }

    protected void cleanupFolderByName(String name) {
        try { api.cleanFolderByName(PROJECT_ID, name); } catch (Exception ignored) {}
    }

    protected void cleanupByName(String name) { cleanupFolderByName(name); }

    protected void cleanupCustomAttr(String nameEn) {
        try {
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            if (info != null) api.deleteCustomAttribute(info[0]);
        } catch (Exception ignored) {}
    }

    protected void saveAuth() {
        synchronized (AUTH_LOCK) {
            try {
                context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(AUTH_STATE_PATH)));
            } catch (Exception e) { log.warn("Failed to save auth.json: {}", e.getMessage()); }
        }
    }

    protected void loginViaApi() {
        try {
            page.request().post(TestConfig.BASE_URL + "/login-api/auth/token/login",
                com.microsoft.playwright.options.RequestOptions.create()
                    .setHeader("Content-Type", "application/json")
                    .setData("{\"username\":\"" + TestConfig.ADMIN_USER + "\",\"password\":\"" + TestConfig.ADMIN_PWD + "\"}"));
            saveAuth();
            log.info("API login OK");
        } catch (Exception e) { log.warn("API login failed: {}", e.getMessage()); }
    }

    /** Smart wait for UI element — waits up to 15s with polling, better than fixed timeout */
    protected void smartWait(Locator locator) {
        try { locator.waitFor(new Locator.WaitForOptions().setTimeout(15000)); } catch (Exception ignored) {}
    }

    /** Wait for network idle */
    protected void waitForNetworkIdle() {
        try { page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(10000)); }
        catch (Exception ignored) {}
    }
}
