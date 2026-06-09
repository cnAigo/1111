package base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import config.TestConfig;
import config.TestConstants;
import pages.RequirementPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/** Base for UI test classes. */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class BaseTest extends AbstractTestBase {

    protected RequirementPage reqPage;

    @Override
    @BeforeAll
    public void setup() {
        super.setup();
        reqPage = new RequirementPage(page);
        navigateToRequirementModule();
    }

    @AfterEach
    void cleanupAfterEach() { closeDialogs(); }

    // ── UI helpers with smart waits ──

    protected void ensureLoggedIn() {
        try {
            String url = page.url();
            if (url != null && url.contains("/login")) {
                log.info("Login page detected, auto-login...");
                page.locator("input[placeholder*='用户名']").first().fill(TestConfig.ADMIN_USER);
                page.locator("input[placeholder*='密码']").first().fill(TestConfig.ADMIN_PWD);
                page.locator("button:has-text('登')").first().click();
                try {
                    page.waitForURL(u -> !u.contains("/login"),
                            new Page.WaitForURLOptions().setTimeout(15000));
                } catch (Exception e) { log.warn("Login redirect timeout"); }
                waitForNetworkIdle();
                saveAuth();
                log.info("Auto-login OK");
            }
        } catch (Exception e) { log.warn("Login check error: {}", e.getMessage()); }
    }

    protected void navigateToRequirementModule() {
        page.navigate(TestConfig.REQUIREMENT_URL);
        waitForNetworkIdle();
        ensureLoggedIn();
    }

    protected void navigateToSystemManagement() {
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        waitForNetworkIdle();
        ensureLoggedIn();
    }

    protected void closeDialogs() {
        try {
            if (page == null) return;
            Locator dlg = page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible");
            if (dlg.count() > 0) {
                page.keyboard().press("Escape");
                try {
                    dlg.first().waitFor(new Locator.WaitForOptions()
                            .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                            .setTimeout(3000));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) { /* dialogs already closed */ }
    }
}
