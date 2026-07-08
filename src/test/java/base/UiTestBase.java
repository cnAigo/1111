package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import config.TestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base for UI test classes. Extends {@link ApiTestBase} with browser + page support.
 * Overrides api to use the browser page's request context (shares cookies/auth state).
 */
public abstract class UiTestBase extends ApiTestBase {

    protected static final String TEST_FILES_DIR = "src/main/resources/testfiles/";

    protected static Browser browser;
    protected static BrowserContext context;
    protected Page page;

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(100));
        context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        page = context.newPage();
        // Re-bind api to use browser page's request context so cookies/auth state is shared
        api = new actions.ReqApiActions(page.request());
    }

    @Override
    @AfterAll
    public void teardownApi() {
        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        super.teardownApi();
    }

    // ── Auth ──

    protected void saveAuth() {
        try {
            context.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get(TestConfig.AUTH_STATE_PATH)));
        } catch (Exception e) {
            log.warn("Failed to save auth state: {}", e.getMessage());
        }
    }

    // ── Wait helpers ──

    protected void waitForNetworkIdle() {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(15000));
        } catch (Exception e) {
            log.warn("Network idle timeout: {}", e.getMessage());
        }
    }

    // ── Screenshot ──

    protected void takeScreenshot(String testName) {
        try {
            Path dir = Paths.get("test-results/screenshots");
            if (!dir.toFile().exists()) dir.toFile().mkdirs();
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(dir.resolve(testName + "_" + System.currentTimeMillis() + ".png"))
                    .setFullPage(true));
        } catch (Exception e) {
            log.warn("Screenshot failed: {}", e.getMessage());
        }
    }

    // ── Dialog management ──

    protected void closeDialogs() {
        try {
            if (page == null) return;
            Locator dlg = page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible");
            if (dlg.count() > 0) {
                page.keyboard().press("Escape");
                try {
                    dlg.first().waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(3000));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { /* dialogs already closed */ }
    }
}
