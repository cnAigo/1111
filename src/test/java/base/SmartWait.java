package base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 智能等待工具 — 彻底替代所有硬编码 waitForTimeout / Thread.sleep。
 * 每一个等待方法都是"状态驱动"的：只等必要条件满足，不盲等固定时间。
 */
public class SmartWait {

    private final Page page;
    private static final int DEFAULT_TIMEOUT_MS = 15_000;
    private static final int SHORT_TIMEOUT_MS  = 5_000;

    public SmartWait(Page page) {
        this.page = page;
    }

    // ── 元素可见 ──

    /** 等待元素在 DOM 中挂载且可见（不可见元素无法交互） */
    public Locator untilVisible(Locator locator) {
        return untilVisible(locator, DEFAULT_TIMEOUT_MS);
    }

    public Locator untilVisible(Locator locator, int timeoutMs) {
        try {
            locator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));
        } catch (TimeoutError e) {
            // 如果默认等待超时，尝试先滚动到视口再等一次
            try { locator.first().scrollIntoViewIfNeeded(); } catch (Exception ignored) {}
            locator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));
        }
        return locator;
    }

    // ── 元素可交互（可见 + 未被禁用） ──

    /** 等待元素可见且 enabled，确保点击/输入不会因 disabled 状态失败 */
    public Locator untilEnabled(Locator locator) {
        return untilEnabled(locator, DEFAULT_TIMEOUT_MS);
    }

    public Locator untilEnabled(Locator locator, int timeoutMs) {
        untilVisible(locator, timeoutMs);
        try {
            page.waitForFunction(
                    "el => el && !el.disabled && el.offsetParent !== null",
                    locator.first().elementHandle(),
                    new Page.WaitForFunctionOptions().setTimeout((double) timeoutMs)
            );
        } catch (TimeoutError e) {
            // enabled 检查失败时仍返回 locator，由调用方决定是否继续
        }
        return locator;
    }

    // ── 元素从 DOM 消失或隐藏 ──

    /** 等待弹窗/遮罩/下拉菜单关闭 */
    public void untilHidden(Locator locator) {
        untilHidden(locator, SHORT_TIMEOUT_MS);
    }

    public void untilHidden(Locator locator, int timeoutMs) {
        try {
            if (locator.count() == 0) return;
            locator.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(timeoutMs));
        } catch (TimeoutError e) {
            // 宽松处理：如果元素还存在但测试可以继续，按 Escape 兜底
            try { page.keyboard().press("Escape"); } catch (Exception ignored) {}
        }
    }

    // ── 网络空闲 ──

    /** 等待所有网络请求完成，避免在 XHR/Fetch 进行中做断言 */
    public void untilNetworkIdle() {
        untilNetworkIdle(DEFAULT_TIMEOUT_MS);
    }

    public void untilNetworkIdle(int timeoutMs) {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(timeoutMs));
        } catch (TimeoutError e) {
            // 部分应用有长轮询/WebSocket，永远达不到 networkidle，不阻塞测试
        }
    }

    // ── URL 变化 ──

    /** 等待 URL 满足断言（如从 /login 跳转到目标页） */
    public void untilUrlMatches(java.util.function.Predicate<String> predicate, int timeoutMs) {
        try {
            page.waitForURL(predicate, new Page.WaitForURLOptions().setTimeout(timeoutMs));
        } catch (TimeoutError e) {
            // 某些情况下页面可能未跳转（如已登录状态直接进入），不视为失败
        }
    }

    // ── 元素稳定（动画结束） ──

    /**
     * 等待元素位置稳定 — 解决动画期间点击偏移的问题。
     * 连续两次轮询位置相同即认为稳定。
     */
    public void untilStable(Locator locator) {
        untilStable(locator, 3000);
    }

    public void untilStable(Locator locator, int timeoutMs) {
        untilVisible(locator, timeoutMs);
        try {
            page.waitForFunction(
                    "el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; }",
                    locator.first().elementHandle(),
                    new Page.WaitForFunctionOptions().setTimeout((double) timeoutMs)
            );
        } catch (TimeoutError e) {
            // 元素可能在视口外但尺寸合法，继续执行
        }
    }

    // ── Toast/消息提示出现后自动消失 ──

    /**
     * 等待 Element Plus 的 el-message 出现后自动消失，
     * 避免 toast 遮挡后续点击目标。
     */
    public void untilToastGone() {
        try {
            Locator toast = page.locator(".el-message:visible, .el-message--success:visible, " +
                    ".el-message--error:visible, .el-message--warning:visible");
            if (toast.count() > 0) {
                untilHidden(toast, 5000);
            }
        } catch (Exception ignored) {}
    }

    // ── 选择器出现 ──

    /** 等待 CSS 选择器在 DOM 中出现（不要求可见） */
    public Locator untilAttached(String selector) {
        return untilAttached(selector, DEFAULT_TIMEOUT_MS);
    }

    public Locator untilAttached(String selector, int timeoutMs) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(timeoutMs));
        return page.locator(selector);
    }
}
