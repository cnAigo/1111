package base;

import actions.ReqApiActions;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public abstract class AbstractTestBase {
    protected static final Logger log = LoggerFactory.getLogger(AbstractTestBase.class);
    protected static final String TEST_FILES_DIR = "src/main/resources/testfiles/";
    protected static Playwright playwright;
    protected static Browser browser;
    protected static BrowserContext context;
    protected Page page;
    protected ReqApiActions api;
    protected String PROJECT_ID;

    @BeforeAll
    public void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(50));
        context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        page = context.newPage();
        api = new ReqApiActions(page.request());
        PROJECT_ID = TestConstants.PROJECT_ID;
        if (PROJECT_ID == null || PROJECT_ID.isBlank()) {
            PROJECT_ID = System.getenv("TAAS_PROJECT_ID");
            if (PROJECT_ID == null || PROJECT_ID.isBlank()) {
                try { PROJECT_ID = api.getProjectIdByName(TestConstants.PROJECT_NAME); }
                catch (Exception e) { PROJECT_ID = "2058851105448046592"; }
            }
            TestConstants.PROJECT_ID = PROJECT_ID;
        }
        log.info("Setup complete. PROJECT_ID={}", PROJECT_ID);
    }

    @AfterAll
    public void teardown() {
        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }

    protected void loginViaApi() { try { AuthHelper.login(page.request(), TestConfig.ADMIN_USER, TestConfig.ADMIN_PWD); } catch (Exception e) { log.warn("API login failed: {}", e.getMessage()); } }
    protected void saveAuth() { try { context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(TestConfig.AUTH_STATE_PATH))); } catch (Exception e) {} }
    protected void waitForNetworkIdle() { try { page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(15000)); } catch (Exception e) {} }
    protected String suffix() { return UUID.randomUUID().toString().replace("-", "").substring(0, 8); }
    protected String resolveParentId() { return PROJECT_ID; }

    protected String[] createTempFolder() {
        String folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
        String folderName = "AT_Folder_" + suffix();
        api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, folderName);
        return new String[]{folderId, folderName};
    }

    protected String[] createTempDoc() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        String docId = api.createDocument(PROJECT_ID, folderId);
        String docName = "AT_Doc_" + suffix();
        api.renameDocument(PROJECT_ID, docId, folderId, docName);
        return new String[]{docId, docName, folderId};
    }

    protected String[] createTempDoc(String ignored) { return createTempDoc(); }
    protected String[] createTempDocFull(String ignored) { return createTempDoc(); }

    protected void cleanupFolderByName(String folderName) { try { api.sweepATFolders(PROJECT_ID); } catch (Exception e) {} }
    protected void cleanupDoc(String docId, String parentId) { try { api.deleteDocument(docId, parentId); } catch (Exception e) {} try { api.forceCleanDocument(docId, parentId); } catch (Exception e) {} }
    protected void cleanupByName(String name) { try { api.sweepATFolders(PROJECT_ID); } catch (Exception e) {} }
    protected void forceCleanFolder(String folderId) { try { api.deleteFolder(folderId, PROJECT_ID, "project"); } catch (Exception e) {} try { api.forceCleanFolder(folderId); } catch (Exception e) {} }
    protected void cleanupCustomAttr(String nameEn) { try { if (nameEn != null) api.deleteCustomAttribute(nameEn); } catch (Exception e) {} }

    protected void takeScreenshot(String testName) {
        try {
            Path dir = Paths.get("test-results/screenshots");
            if (!dir.toFile().exists()) dir.toFile().mkdirs();
            page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve(testName + "_" + System.currentTimeMillis() + ".png")).setFullPage(true));
        } catch (Exception e) {}
    }

    protected void closeDialogs() {
        try { if (page != null) { Locator dlg = page.locator(".el-dialog:visible, .el-overlay:visible"); if (dlg.count() > 0) { page.keyboard().press("Escape"); } } } catch (Exception e) {}
    }
}
