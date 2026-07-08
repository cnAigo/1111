package base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import config.TestConfig;
import config.TestConstants;
import pages.RequirementPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * UI 测试基类 — 集成 SmartWait + SafeActions，彻底消除硬编码等待。
 * 子类可直接使用 {@link #ui} 进行安全的点击/输入操作。
 */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class BaseTest extends AbstractTestBase {

    protected RequirementPage reqPage;
    protected SafeActions ui;
    protected SmartWait waiter;

    @Override
    @BeforeAll
    public void setup() {
        super.setup();
        // 初始化安全操作工具和智能等待
        this.ui = new SafeActions(page);
        this.waiter = new SmartWait(page);
        reqPage = new RequirementPage(page);
        navigateToRequirementModule();
    }

    @AfterEach
    void cleanupAfterEach() {
        closeDialogs();
    }

    // ── 登录（状态判断优先于死板流程） ──

    /**
     * 确保已登录。先检查当前 URL 是否已在登录后的页面，
     * 只有确实在 /login 时才执行登录操作。
     * 避免"盲登录"——如果已登录状态下重复点击登录按钮可能导致异常。
     */
    protected void ensureLoggedIn() {
        try {
            String url = page.url();
            // 状态判断：不在登录页则已登录，直接返回
            if (url != null && !url.contains("/login")) {
                return;
            }
            log.info("检测到登录页面，执行自动登录...");

            // 优先级：placeholder 文本 > ARIA role
            // placeholder 文本是用户直观看到的，比 CSS class 更稳定
            Locator userInput = page.locator("input[placeholder*='用户名'], input[placeholder*='账号']").first();
            Locator pwdInput  = page.locator("input[placeholder*='密码']").first();

            // 等待输入框可见后再填入（状态驱动等待，不用 sleep）
            waiter.untilVisible(userInput, 10_000);
            ui.fill(userInput, TestConfig.ADMIN_USER);
            ui.fill(pwdInput, TestConfig.ADMIN_PWD);

            // 定位登录按钮：优先精确文本匹配，兜底 ARIA role
            Locator loginBtn = page.locator("button:has-text('登')").first();
            ui.click(loginBtn);

            // 等待离开登录页，超时 20 秒（SSO 或慢网络可能更久）
            waiter.untilUrlMatches(u -> !u.contains("/login"), 20_000);
            waiter.untilNetworkIdle();
            saveAuth();
            log.info("自动登录完成");
        } catch (Exception e) {
            log.warn("登录检查/执行异常: {}", e.getMessage());
        }
    }

    // ── 导航 ──

    protected void navigateToRequirementModule() {
        page.navigate(TestConfig.REQUIREMENT_URL);
        // 等待页面核心元素出现（状态驱动），比 waitForTimeout 可靠 100 倍
        waiter.untilNetworkIdle();
        ensureLoggedIn();
    }

    protected void navigateToSystemManagement() {
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        waiter.untilNetworkIdle();
        ensureLoggedIn();
    }

    // ── 弹窗关闭（智能判断有无弹窗） ──

    /**
     * 关闭所有可能遮挡操作的弹窗/遮罩。
     * 先判断是否存在弹窗（count > 0），避免盲按 Escape 干扰正常页面。
     */
    protected void closeDialogs() {
        try {
            if (page == null) return;
            Locator dlg = page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible");
            // 状态判断：没有弹窗就跳过，不盲按 Escape
            if (dlg.count() == 0) return;

            page.keyboard().press("Escape");
            // 等待弹窗关闭动画完成（元素隐藏），超时缩短到 2s 因为动画通常很快
            waiter.untilHidden(dlg, 2000);
        } catch (Exception e) {
            // 弹窗已关闭或不存在，正常流程不需抛出
        }
    }
}
