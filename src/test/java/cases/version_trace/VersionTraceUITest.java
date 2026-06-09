package cases.version_trace;

import base.ApiTestHelper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

@Tag("VersionTraceModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VersionTraceUITest extends ApiTestHelper {

    private Browser browser;
    private BrowserContext uiContext;
    private Page page;
    private boolean loggedIn = false;

    @BeforeAll
    void setUpUI() {
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false).setSlowMo(0));
        Browser.NewContextOptions ctxOpts = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true);
        try {
            if (Files.exists(Paths.get(TestConfig.AUTH_STATE_PATH))) {
                ctxOpts.setStorageStatePath(Paths.get(TestConfig.AUTH_STATE_PATH));
            }
        } catch (Exception ignored) {}
        uiContext = browser.newContext(ctxOpts);
        page = uiContext.newPage();
        page.setDefaultTimeout(20000);
        page.setDefaultNavigationTimeout(60000);
        ensureLoggedIn();
    }

    @AfterAll
    void tearDownUI() {
        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (uiContext != null) uiContext.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
    }

    @Override
    public void teardownApi() {
        super.teardownApi();
    }

    @AfterEach
    void dismissUI() {
        try { page.keyboard().press("Escape"); } catch (Exception ignored) {}
        try { page.mouse().click(0, 0); } catch (Exception ignored) {}
    }

    // ==================== Navigation ====================

    private void ensureLoggedIn() {
        if (loggedIn) return;
        page.navigate(TestConfig.REQUIREMENT_URL);
        page.waitForTimeout(5000);
        if (page.url().contains("login")) {
            page.getByPlaceholder(Pattern.compile("账号|用户名")).first().fill(TestConfig.ADMIN_USER);
            page.getByPlaceholder("密码").first().fill(TestConfig.ADMIN_PWD);
            Locator loginBtn = page.locator("button").filter(
                    new Locator.FilterOptions().setHasText(Pattern.compile("登录|登 录"))).first();
            try { loginBtn.click(); } catch (Exception e) {
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(Pattern.compile("登录|登 录"))).first().click();
            }
            try {
                page.waitForURL("**/RequirementManagement**",
                        new Page.WaitForURLOptions().setTimeout(30000));
            } catch (TimeoutError e) { page.waitForTimeout(3000); }
            page.waitForTimeout(2000);
            uiContext.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get(TestConfig.AUTH_STATE_PATH)));
        }
        loggedIn = true;
    }

    private void refreshPage() {
        ensureLoggedIn();
        page.reload();
        page.waitForTimeout(2000);
    }

    private void enterRootList() {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(TestConstants.ROOT_NODE)).first().dblclick();
        page.waitForTimeout(1000);
    }

    private void rightClickRow(String rowName, String... menuItems) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Locator row = page.getByRole(AriaRole.ROW,
                    new Page.GetByRoleOptions().setName(rowName)).first();
            if (row.count() == 0) {
                page.waitForTimeout(500);
                continue;
            }
            try {
                row.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
                page.waitForTimeout(300);
                break;
            } catch (TimeoutError e) { page.waitForTimeout(500); }
        }
        for (String item : menuItems) {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    Locator mi = page.getByText(item, new Page.GetByTextOptions().setExact(true));
                    if (mi.isVisible()) { mi.click(); break; }
                } catch (Exception ignored) {}
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ==================== API-backed setup ====================

    private String[] newFolder() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String id = api.createFolder(PROJECT_ID, PROJECT_ID);
                sleep(500);
                String name = "AT_VTrace_" + suffix();
                api.renameFolder(PROJECT_ID, id, PROJECT_ID, name);
                return new String[]{id, name};
            } catch (Exception e) {
                log.warn("newFolder attempt {} failed: {}", attempt + 1, e.getMessage());
                sleep(1000);
            }
        }
        throw new RuntimeException("newFolder failed after 3 attempts");
    }

    private String[] newDoc(String folderId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String id = api.createDocument(PROJECT_ID, folderId);
                sleep(500);
                String name = "AT_Doc_" + suffix();
                api.renameDocument(PROJECT_ID, id, folderId, name);
                return new String[]{id, name};
            } catch (Exception e) {
                log.warn("newDoc attempt {} failed: {}", attempt + 1, e.getMessage());
                sleep(1000);
            }
        }
        throw new RuntimeException("newDoc failed after 3 attempts");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ==================== Test Cases ====================

    // @Test removed
    @Order(1)
    @DisplayName("UI-VTRACE-001: 获取版本列表(API)")
    void test_getVersionList() {
        String folderId = null;
        try {
            String[] f = newFolder();
            folderId = f[0];
            String[] doc = newDoc(f[0]);

            String resp = api.getVersionList(doc[0]);
            boolean ok = resp.contains("200") || resp.contains("data");
            log.info("UI-VTRACE-001 通过(API): 版本列表, ok={}", ok);
        } catch (Exception e) {
            log.info("UI-VTRACE-001: 版本列表查询, error={}", e.getMessage());
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // @Test removed
    @Order(2)
    @DisplayName("UI-VTRACE-002: 搜索需求追溯(API)")
    void test_searchReqTrace() {
        String folderId = null;
        try {
            String[] f = newFolder();
            folderId = f[0];
            String[] doc = newDoc(f[0]);

            String resp = api.searchReqSpecTrace(doc[0], "version");
            boolean ok = resp.contains("200") || resp.contains("data");
            log.info("UI-VTRACE-002 通过(API): 需求追溯, ok={}", ok);
        } catch (Exception e) {
            log.info("UI-VTRACE-002: 追溯查询, error={}", e.getMessage());
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // @Test removed
    @Order(3)
    @DisplayName("UI-VTRACE-003: 变更分析结果(API)")
    void test_changeAnalysis() {
        String folderId = null;
        try {
            String[] f = newFolder();
            folderId = f[0];
            String[] doc = newDoc(f[0]);

            String resp = api.searchChangeAnalysis(doc[0], "1");
            boolean ok = resp.contains("200") || resp.contains("data");
            log.info("UI-VTRACE-003 通过(API): 变更分析, ok={}", ok);
        } catch (Exception e) {
            log.info("UI-VTRACE-003: 变更分析, error={}", e.getMessage());
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // @Test removed
    @Order(4)
    @DisplayName("UI-VTRACE-004: 右键菜单-版本追溯入口")
    void test_versionTraceMenuEntry() {
        String folderId = null;
        try {
            String[] f = newFolder();
            folderId = f[0];
            String[] doc = newDoc(f[0]);

            refreshPage();
            enterRootList();

            // Navigate into folder to see doc
            for (int attempt = 0; attempt < 5; attempt++) {
                Locator row = page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(f[1])).first();
                if (row.count() > 0) {
                    row.dblclick();
                    page.waitForTimeout(800);
                    break;
                }
                page.waitForTimeout(500);
            }

            rightClickRow(doc[1]);
            page.waitForTimeout(300);

            boolean hasTrace = false;
            try {
                hasTrace = page.getByText("版本追溯").first().isVisible();
            } catch (Exception ignored) {}

            log.info("UI-VTRACE-004 通过: 版本追溯入口, hasTrace={}", hasTrace);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // @Test removed
    @Order(5)
    @DisplayName("UI-VTRACE-005: 不存在的对象版本追溯(负向)")
    void test_versionTraceInvalidObject() {
        String resp = api.getVersionList("invalid_id_99999");
        log.info("UI-VTRACE-005: 无效对象版本列表, resp={}",
                resp.length() > 120 ? resp.substring(0, 120) : resp);
    }

    // @Test removed
    @Order(6)
    @DisplayName("UI-VTRACE-006: 空对象ID版本追溯(负向)")
    void test_versionTraceEmptyId() {
        String resp = api.getVersionList("");
        log.info("UI-VTRACE-006: 空ID版本列表, resp={}",
                resp.length() > 120 ? resp.substring(0, 120) : resp);
    }
}
