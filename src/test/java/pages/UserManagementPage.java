package pages;

import base.SafeActions;
import base.SmartWait;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for 用户管理 (User Management) page.
 * 已重构：所有 waitForTimeout 替换为状态驱动等待，
 * 所有 click/fill 改用 SafeActions 封装。
 */
public class UserManagementPage {

    private final Page page;
    private final SafeActions ui;
    private final SmartWait waiter;

    public UserManagementPage(Page page) {
        this.page = page;
        this.ui = new SafeActions(page);
        this.waiter = new SmartWait(page);
    }

    // ==================== Navigation ====================

    @Step("点击侧边栏菜单: 用户管理")
    public void navigateToUserManagement() {
        expandSidebarParent("系统管理");
        clickSidebarItem("用户管理");
    }

    /**
     * 展开侧边栏父级菜单项。
     * 先检查 aria-expanded 状态，已展开则不重复点击（状态判断优先于盲操作）。
     */
    private void expandSidebarParent(String parentName) {
        try {
            // 定位器优先使用可见文本，次用 sub-menu CSS（Element Plus 的稳定 class）
            Locator parent = page.locator(".el-sub-menu__title, [class*='submenu']")
                    .filter(new Locator.FilterOptions().setHasText(parentName)).first();
            if (parent.count() == 0) return;

            waiter.untilVisible(parent, 8000);
            String ariaExpanded = parent.getAttribute("aria-expanded");
            // 状态判断：已展开则跳过点击
            if ("true".equals(ariaExpanded)) return;

            ui.click(parent);
            // 等待子菜单出现（状态驱动：等待菜单项的 CSS transition 结束）
            waiter.untilAttached(
                    ".el-menu--inline:visible, [role='menuitem']:visible", 3000);
        } catch (Exception ignored) {
            // 侧边栏可能已展开或菜单结构不同，放行
        }
    }

    /**
     * 点击侧边栏子菜单项。
     * 等待目标元素可见后再点击，点击后等待表格/页面内容渲染完成。
     */
    private void clickSidebarItem(String itemName) {
        Locator item = page.locator(".el-menu-item, [role='menuitem'], li")
                .filter(new Locator.FilterOptions().setHasText(itemName)).first();
        // 等待菜单项可见（状态驱动，不用 sleep）
        waiter.untilVisible(item, 10_000);
        ui.click(item);
        // 点击菜单后等待页面内容开始渲染（表格出现或页面切换完成）
        waiter.untilNetworkIdle();
    }

    // ==================== Locators ====================

    /** 新增按钮 — 文本匹配比 CSS class 更稳定，适应 UI 改版 */
    private Locator addButton() {
        return page.locator("button").filter(
                new Locator.FilterOptions().setHasText(Pattern.compile("新增|添加"))).first();
    }

    private Locator editButton() {
        return page.locator("button").filter(
                new Locator.FilterOptions().setHasText("修改")).first();
    }

    private Locator deleteButton() {
        return page.locator("button").filter(
                new Locator.FilterOptions().setHasText("删除")).first();
    }

    /** 表格行 — ARIA role 比 CSS 类更语义化、更稳定 */
    private Locator tableRow(String name) {
        return page.getByRole(AriaRole.ROW,
                new Page.GetByRoleOptions().setName(name)).first();
    }

    // ==================== Dialog Form ====================

    @Step("填写用户昵称: {nickname}")
    public void fillNickname(String nickname) {
        fillFormField("用户昵称", nickname);
    }

    @Step("填写用户名称: {username}")
    public void fillUserName(String username) {
        fillFormField("用户名称", username);
    }

    @Step("填写用户密码: {password}")
    public void fillPassword(String password) {
        fillFormField("用户密码", password);
    }

    @Step("填写手机号码: {phone}")
    public void fillPhone(String phone) {
        fillFormField("手机号码", phone);
    }

    @Step("填写邮箱: {email}")
    public void fillEmail(String email) {
        fillFormField("邮箱", email);
    }

    /**
     * 选择下拉选项 — 点击 Select → 等待选项列表出现 → 点击目标选项 → 等待列表消失。
     * 不再用 waitForTimeout 盲等，改为等待 option 角色出现。
     */
    @Step("选择用户性别: {gender}")
    public void selectGender(String gender) {
        // 点击 Select 触发器
        Locator trigger = dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("用户性别"))
                .locator("input, .el-select").first();
        ui.click(trigger);
        // 等待下拉选项渲染（状态驱动）
        ui.selectOption(gender);
    }

    @Step("选择状态: {status}")
    public void selectStatus(String status) {
        Locator trigger = dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("状态"))
                .locator("input, .el-select").first();
        ui.click(trigger);
        ui.selectOption(status);
    }

    /**
     * 填充表单字段 — 通过 label 文本定位对应的 input。
     * 文本关联比 CSS 层级更稳定。
     */
    private void fillFormField(String labelText, String value) {
        Locator input = dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText(labelText))
                .locator("input, textarea").first();
        ui.fill(input, value);
    }

    /** 获取当前可见对话框 */
    private Locator dialog() {
        return page.locator(".el-dialog:visible").first();
    }

    // ==================== Actions ====================

    @Step("点击新增按钮")
    public void clickAdd() {
        ui.click(addButton());
        // 等待对话框出现（状态驱动）
        waiter.untilAttached(".el-dialog:visible, .el-drawer:visible", 5000);
    }

    @Step("点击修改按钮")
    public void clickEdit() {
        ui.click(editButton());
        waiter.untilAttached(".el-dialog:visible, .el-drawer:visible", 5000);
    }

    @Step("点击删除按钮")
    public void clickDelete() {
        ui.click(deleteButton());
        // 等待确认对话框出现
        waiter.untilAttached(".el-message-box:visible", 3000);
    }

    @Step("点击对话框确定按钮")
    public void clickConfirm() {
        Locator confirmBtn = page.locator(
                ".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确"))).first();
        ui.click(confirmBtn);
        // 等待对话框关闭（状态驱动）而不是 sleep
        waiter.untilHidden(page.locator(".el-dialog:visible, .el-message-box:visible"), 5000);
        // 等待可能的 toast 消息消失，避免遮挡后续操作
        waiter.untilToastGone();
    }

    @Step("点击对话框取消按钮")
    public void clickCancel() {
        Locator cancelBtn = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("取消"));
        ui.click(cancelBtn);
        waiter.untilHidden(page.locator(".el-dialog:visible"), 3000);
    }

    @Step("点击确认删除")
    public void clickDeleteConfirm() {
        Locator confirm = page.locator("button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确定|确 定")));
        if (confirm.count() > 0 && confirm.isVisible()) {
            ui.click(confirm);
            waiter.untilToastGone();
        }
    }

    @Step("搜索用户: userName={userName}, phone={phone}")
    public void search(String userName, String phone) {
        if (userName != null && !userName.isEmpty()) {
            // 使用 placeholder 文本定位 — 这是用户直接看到的属性，比 CSS 路径稳定
            Locator nameInput = page.locator("input[placeholder*='用户名称']").first();
            ui.fill(nameInput, userName);
        }
        if (phone != null && !phone.isEmpty()) {
            Locator phoneInput = page.locator("input[placeholder*='手机号码']").first();
            ui.fill(phoneInput, phone);
        }
        // 点击搜索按钮
        Locator searchBtn = page.locator("button")
                .filter(new Locator.FilterOptions().setHasText("搜索")).first();
        ui.click(searchBtn);
        // 等待搜索结果加载（表格行更新），而不是 sleep
        waiter.untilNetworkIdle();
    }

    @Step("在表格行中点击操作: {rowName} → {buttonText}")
    public void clickRowAction(String rowName, String buttonText) {
        Locator row = tableRow(rowName);
        waiter.untilVisible(row, 5000);
        Locator actionBtn = row.locator("button, a")
                .filter(new Locator.FilterOptions().setHasText(buttonText)).first();
        ui.click(actionBtn);
        // 等待对话框或确认弹窗出现
        waiter.untilAttached(
                ".el-dialog:visible, .el-message-box:visible, .el-drawer:visible", 3000);
    }

    @Step("点击操作列中的重置密码: {rowName}")
    public void clickResetPassword(String rowName) {
        clickRowAction(rowName, "重置密码");
    }

    // ==================== Assertions ====================

    @Step("断言用户行可见: {name}")
    public void assertRowVisible(String name) {
        Locator row = tableRow(name);
        waiter.untilVisible(row);
        assertThat(row).isVisible();
    }

    @Step("断言Toast消息包含: {text}")
    public void assertToastContains(String text) {
        Locator toast = page.locator(
                ".el-message--success, .el-message--error, .el-message__content").first();
        waiter.untilVisible(toast, 5000);
        assertThat(toast).containsText(text);
    }

    @Step("获取Toast消息")
    public String getToastMessage() {
        try {
            Locator toast = page.locator(
                    ".el-message--success, .el-message--error, .el-message--warning, " +
                    ".el-message__content").first();
            // 等待 toast 出现再读取，但 toast 消失很快，用短超时避免空等
            try {
                waiter.untilVisible(toast, 3000);
            } catch (TimeoutError ignored) {
                // toast 可能已经出现并消失，尝试直接读取
            }
            if (toast.count() > 0) return toast.textContent();
        } catch (Exception ignored) {}
        return "";
    }
}
