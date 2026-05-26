package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import config.TestConfig;
import config.TestConstants;
import base.SetupEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
@ExtendWith(TimeoutSkipExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    private static final String AUTH_STATE_PATH = "auth.json";

    @BeforeAll
    public void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false).setSlowMo(0));

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true);

        java.nio.file.Path authPath = Paths.get(AUTH_STATE_PATH);
        if (java.nio.file.Files.exists(authPath)) {
            contextOptions.setStorageStatePath(authPath);
            log.info("Detected auth.json, loading session...");
        }

        context = browser.newContext(contextOptions);
        page = context.newPage();
        page.setDefaultTimeout(10000);
        page.setDefaultNavigationTimeout(30000);
       // SetupEnvironment.createAll();
    }

    @AfterAll
    public void teardown() {
        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
    }

    protected void ensureLoggedIn() {
        try {
            String currentUrl = page.url();
            if (currentUrl != null && currentUrl.contains("/login")) {
                log.info("检测到登录页面，正在自动登录...");
                page.locator("input[placeholder*='用户名']").first().fill(TestConfig.ADMIN_USER);
                page.waitForTimeout(200);
                page.locator("input[placeholder*='密码']").first().fill(TestConfig.ADMIN_PWD);
                page.waitForTimeout(200);
                page.locator("button:has-text('登')").first().click();
                try {
                    page.waitForURL(url -> !url.contains("/login"), new Page.WaitForURLOptions().setTimeout(10000));
                    // 或者等待首页的某个标志性组件加载出来，比如左侧菜单栏
                    // page.locator(".el-menu").first().waitFor();
                } catch (Exception e) {
                    log.warn("等待登录跳转超时，尝试继续执行...");
                }

// 登录成功后稍微稳一下，再保存状态
                page.waitForTimeout(2000);
                context.storageState(new BrowserContext.StorageStateOptions().setPath(Paths.get(AUTH_STATE_PATH)));
                log.info("自动登录成功，认证状态已保存到 {}", AUTH_STATE_PATH);
            }
        } catch (Exception e) {
            log.warn("检查登录状态时出错: {}", e.getMessage());
        }
    }

    protected void navigateToRequirementModule() {
        page.navigate(TestConfig.REQUIREMENT_URL);
        page.waitForTimeout(2000);
        ensureLoggedIn();

        Locator projectDropdown = page.locator(".el-dropdown .el-tag__content").first();
        if (projectDropdown.isVisible()) {
            projectDropdown.click();
            page.waitForTimeout(200);
            page.getByRole(AriaRole.MENUITEM,
                    new Page.GetByRoleOptions().setName(TestConstants.PROJECT_NAME)).click();
            page.waitForTimeout(500);
            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹")).locator("svg").click();
        }
    }

    protected void navigateToSystemManagement() {
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        page.waitForTimeout(1500);
        ensureLoggedIn();
    }

    @org.junit.jupiter.api.AfterEach
    void cleanupAfterEach() {
        try {
            while (page != null &&
                    page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible").count() > 0) {
                page.keyboard().press("Escape");
                page.waitForTimeout(300);
            }
        } catch (Exception e) {
            log.warn("清理残留弹窗异常: {}", e.getMessage());
        }
    }

}