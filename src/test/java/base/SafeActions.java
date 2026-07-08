package base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 高鲁棒性行为封装 — 替代原生 click() / fill() / dblclick()。
 *
 * 每一次交互都内置：
 *   1. 可见性检查（元素存在且可见）
 *   2. 滚动到视口（scrollIntoViewIfNeeded）
 *   3. 全局弹窗/遮罩自动关闭（Cookie 弹窗、公告、Element Plus 遮罩）
 *   4. 失败自动重试（被遮挡 → 关遮罩 → 重试）
 */
public class SafeActions {

    private static final Logger log = LoggerFactory.getLogger(SafeActions.class);
    private final Page page;
    private final SmartWait wait;

    /** 最大重试次数：处理因弹窗遮挡、动画未完成导致的点击失败 */
    private static final int MAX_RETRIES = 3;

    public SafeActions(Page page) {
        this.page = page;
        this.wait = new SmartWait(page);
    }

    // ═══════════════════════════════════════════════════
    // 安全点击
    // ═══════════════════════════════════════════════════

    /** 安全点击 — 自动等待可见 → 滚入视口 → 关闭遮挡 → 点击 → 失败重试 */
    public void click(Locator locator) {
        click(locator, MAX_RETRIES);
    }

    public void click(Locator locator, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // ① 等待目标元素可见且稳定（动画结束）
                wait.untilVisible(locator);
                wait.untilStable(locator);
                // ② 滚动到视口内，避免元素在屏幕外导致点击落空
                locator.first().scrollIntoViewIfNeeded();
                // ③ 关闭可能遮挡目标的全局弹窗 / Cookie 横幅
                dismissFloatingOverlays();
                // ④ 执行点击
                locator.first().click(new Locator.ClickOptions().setTimeout(10_000));
                return; // 点击成功，退出重试循环
            } catch (Exception e) {
                if (attempt < maxRetries - 1) {
                    log.warn("点击失败(第{}次尝试), 关闭遮挡后重试: {}",
                            attempt + 1, e.getMessage());
                    // ⑤ 兜底：强制关闭所有遮罩层，避免遮挡导致连续失败
                    forceDismissAllOverlays();
                } else {
                    // 所有重试耗尽 — 抛出异常让测试框架感知失败
                    throw new RuntimeException(
                            "安全点击失败(已重试" + maxRetries + "次): " + e.getMessage(), e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 安全双击
    // ═══════════════════════════════════════════════════

    /** 安全双击 — 用于树节点展开、单元格编辑激活等需要双击的场景 */
    public void dblClick(Locator locator) {
        dblClick(locator, MAX_RETRIES);
    }

    public void dblClick(Locator locator, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                wait.untilVisible(locator);
                wait.untilStable(locator);
                locator.first().scrollIntoViewIfNeeded();
                dismissFloatingOverlays();
                locator.first().dblclick(new Locator.DblclickOptions().setTimeout(10_000));
                return;
            } catch (Exception e) {
                if (attempt < maxRetries - 1) {
                    log.warn("双击失败(第{}次尝试), 重试中: {}", attempt + 1, e.getMessage());
                    forceDismissAllOverlays();
                } else {
                    throw new RuntimeException(
                            "安全双击失败(已重试" + maxRetries + "次): " + e.getMessage(), e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 安全输入
    // ═══════════════════════════════════════════════════

    /**
     * 安全输入 — 等待输入框可见 → 点击聚焦 → 全选清空 → 填入新值。
     * 不直接 fill() 的原因：部分前端框架（如 Element Plus）需要 focus 事件才能
     * 正确绑定数据模型，直接 fill 可能导致表单校验不触发或值未同步。
     */
    public void fill(Locator locator, String value) {
        fill(locator, value, MAX_RETRIES);
    }

    public void fill(Locator locator, String value, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                wait.untilVisible(locator);
                wait.untilEnabled(locator);
                locator.first().scrollIntoViewIfNeeded();
                dismissFloatingOverlays();
                // 先点击聚焦，确保输入框获得焦点
                locator.first().click(new Locator.ClickOptions().setTimeout(5000));
                // 全选当前内容后填入新值（处理已有默认值的情况）
                locator.first().press("Control+a");
                locator.first().fill(value);
                return;
            } catch (Exception e) {
                if (attempt < maxRetries - 1) {
                    log.warn("输入失败(第{}次尝试), 重试中: {}", attempt + 1, e.getMessage());
                    forceDismissAllOverlays();
                } else {
                    throw new RuntimeException(
                            "安全输入失败(已重试" + maxRetries + "次): " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 安全输入（不清空） — 适用于搜索框等需要在已有内容后追加的场景。
     * 不会全选已有内容，直接 fill 覆盖。
     */
    public void fillWithoutClear(Locator locator, String value) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                wait.untilVisible(locator);
                locator.first().scrollIntoViewIfNeeded();
                dismissFloatingOverlays();
                locator.first().fill(value);
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES - 1) {
                    log.warn("fill失败(第{}次尝试), 重试中", attempt + 1);
                } else {
                    throw new RuntimeException("安全输入失败: " + e.getMessage(), e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 安全右键
    // ═══════════════════════════════════════════════════

    /** 安全右键点击 — 用于触发上下文菜单 */
    public void rightClick(Locator locator) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                wait.untilVisible(locator);
                wait.untilStable(locator);
                locator.first().scrollIntoViewIfNeeded();
                dismissFloatingOverlays();
                locator.first().click(new Locator.ClickOptions()
                        .setButton(com.microsoft.playwright.options.MouseButton.RIGHT)
                        .setTimeout(10_000));
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES - 1) {
                    log.warn("右键失败(第{}次尝试)", attempt + 1);
                    forceDismissAllOverlays();
                } else {
                    throw new RuntimeException("安全右键失败: " + e.getMessage(), e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 安全选择（下拉选项）
    // ═══════════════════════════════════════════════════

    /** 点击下拉选项 — 等待选项列表出现后再点击目标选项 */
    public void selectOption(String optionText) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // 等待下拉选项出现在 DOM 中
                Locator option = page.getByRole(AriaRole.OPTION,
                        new Page.GetByRoleOptions().setName(optionText));
                wait.untilVisible(option, 5000);
                option.first().click();
                // 等待选项列表消失，确认选择已生效
                try {
                    page.locator("[role=option]").first().waitFor(
                            new Locator.WaitForOptions()
                                    .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                                    .setTimeout(3000));
                } catch (TimeoutError ignored) {}
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES - 1) {
                    log.warn("选择选项'{}'失败(第{}次尝试)", optionText, attempt + 1);
                } else {
                    throw new RuntimeException(
                            "选择选项'" + optionText + "'失败: " + e.getMessage(), e);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 遮挡消除
    // ═══════════════════════════════════════════════════

    /**
     * 关闭常见的浮动遮挡层。
     * 不关闭模态对话框（那是用户主动打开的，不应盲关），只关闭：
     *   - Cookie / 隐私声明横幅
     *   - 全局通知 / 公告弹窗
     *   - 非模态的悬浮提示
     */
    private void dismissFloatingOverlays() {
        try {
            // ① 尝试关闭 Element Plus 的全局消息提示（非模态，但可能遮挡按钮）
            Locator toast = page.locator(
                    ".el-message--success:visible, .el-message--warning:visible, .el-notification:visible");
            if (toast.count() > 0) {
                // 点击页面空白区域让 toast 消失
                page.mouse().click(0, 0);
            }
        } catch (Exception ignored) {}
        try {
            // ② 关闭常见的 Cookie 同意弹窗（按 Escape 或点击空白）
            Locator cookieBar = page.locator(
                    "[class*=cookie]:visible, [id*=cookie]:visible, [class*=gdpr]:visible");
            if (cookieBar.count() > 0) {
                try {
                    Locator acceptBtn = cookieBar.locator(
                            "button:has-text('接受'), button:has-text('同意'), button:has-text('Accept'), button:has-text('OK')");
                    if (acceptBtn.count() > 0) acceptBtn.first().click();
                } catch (Exception e) {
                    page.keyboard().press("Escape");
                }
            }
        } catch (Exception ignored) {}
    }

    /** 强制关闭所有遮罩（包括模态弹窗）— 仅在重试失败时作为兜底手段 */
    private void forceDismissAllOverlays() {
        try {
            // 按 Escape 关闭最上层的模态弹窗
            page.keyboard().press("Escape");
            // 短暂等待弹窗关闭动画
            try {
                page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible")
                        .first().waitFor(new Locator.WaitForOptions()
                                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                                .setTimeout(2000));
            } catch (TimeoutError ignored) {}
        } catch (Exception ignored) {}
        try {
            // 点击页面左上角空白区域，消除可能的焦点/悬浮状态
            page.mouse().click(0, 0);
        } catch (Exception ignored) {}
    }
}
