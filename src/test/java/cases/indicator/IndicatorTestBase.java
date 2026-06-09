package cases.indicator;

import actions.IndicatorApiActions;
import base.UiTestBase;
import com.microsoft.playwright.Page;
import config.TestConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import base.TimeoutSkipExtension;

/**
 * Base class for indicator tests — provides IndicatorApiActions instance
 * and cleanup helpers.  Extends UiTestBase for auth + browser lifecycle.
 */
@ExtendWith(TimeoutSkipExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndicatorTestBase extends UiTestBase {

    protected IndicatorApiActions ind;

    @BeforeAll
    @Override
    public void setupApi() {
        super.setupApi();
        // Indicator module (/api-api) needs full browser SSO login
        try {
            page.navigate(TestConfig.BASE_URL + "/#/login");
            page.waitForLoadState();
            Thread.sleep(2000);
            if (page.locator("input[type='password']").count() > 0) {
                page.locator("input[type='text']").first().fill(TestConfig.ADMIN_USER);
                page.locator("input[type='password']").first().fill(TestConfig.ADMIN_PWD);
                page.locator("button[type='submit']").first().click();
                page.waitForURL(u -> !u.contains("/login"), new Page.WaitForURLOptions().setTimeout(30000));
                log.info("Browser SSO login OK for indicator module");
                saveAuth();
            }
        } catch (Exception e) {
            log.warn("Browser SSO login failed: {}, trying API-only", e.getMessage());
        }
        ind = new IndicatorApiActions(context.request(), PROJECT_ID);
    }
}
