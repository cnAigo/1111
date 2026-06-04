package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for 用户管理 (User Management) page.
 * Accessed via SystemManagement sidebar menu → 用户管理.
 *
 * <p>Table headers: 用户编号, 用户名称, 用户昵称, 部门, 手机号码, 状态, 密级, 创建时间, 操作</p>
 */
public class UserManagementPage {

    private final Page page;

    public UserManagementPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    @Step("点击侧边栏菜单: 用户管理")
    public void navigateToUserManagement() {
        expandSidebarParent("系统管理");
        clickSidebarItem("用户管理");
    }

    private void expandSidebarParent(String parentName) {
        try {
            Locator parent = page.locator(".el-sub-menu__title, .el-submenu__title, [class*='submenu']")
                    .filter(new Locator.FilterOptions().setHasText(parentName)).first();
            if (parent.count() > 0 && parent.isVisible()) {
                String ariaExpanded = parent.getAttribute("aria-expanded");
                if (!"true".equals(ariaExpanded)) {
                    parent.click();
                    page.waitForTimeout(500);
                }
            }
        } catch (Exception ignored) {}
    }

    private void clickSidebarItem(String itemName) {
        Locator item = page.locator(".el-menu-item, [role='menuitem'], li")
                .filter(new Locator.FilterOptions().setHasText(itemName)).first();
        item.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        item.click();
        page.waitForTimeout(1500);
    }

    // ==================== Locators ====================

    private Locator addButton() {
        return page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("新增|添加")))
                .first();
    }

    private Locator editButton() {
        return page.locator("button").filter(new Locator.FilterOptions().setHasText("修改")).first();
    }

    private Locator deleteButton() {
        return page.locator("button").filter(new Locator.FilterOptions().setHasText("删除")).first();
    }

    private Locator tableRow(String name) {
        return page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(name)).first();
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

    @Step("选择用户性别: {gender}")
    public void selectGender(String gender) {
        dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("用户性别"))
                .locator("input, .el-select").first().click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(gender)).click();
        page.waitForTimeout(300);
    }

    @Step("选择状态: {status}")
    public void selectStatus(String status) {
        dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("状态"))
                .locator("input, .el-select").first().click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(status)).click();
        page.waitForTimeout(300);
    }

    private void fillFormField(String labelText, String value) {
        Locator input = dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText(labelText))
                .locator("input, textarea").first();
        input.click();
        input.press("Control+a");
        input.fill(value);
    }

    private Locator dialog() {
        return page.locator(".el-dialog:visible").first();
    }

    // ==================== Actions ====================

    @Step("点击新增按钮")
    public void clickAdd() {
        addButton().click();
        page.waitForTimeout(800);
    }

    @Step("点击修改按钮")
    public void clickEdit() {
        editButton().click();
        page.waitForTimeout(800);
    }

    @Step("点击删除按钮")
    public void clickDelete() {
        deleteButton().click();
        page.waitForTimeout(500);
    }

    @Step("点击对话框确定按钮")
    public void clickConfirm() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确"))).first().click();
        page.waitForTimeout(800);
    }

    @Step("点击对话框取消按钮")
    public void clickCancel() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(300);
    }

    @Step("点击确认删除")
    public void clickDeleteConfirm() {
        Locator confirm = page.locator("button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确定|确 定")));
        if (confirm.isVisible()) confirm.click();
        page.waitForTimeout(500);
    }

    @Step("搜索用户: userName={userName}, phone={phone}")
    public void search(String userName, String phone) {
        if (userName != null && !userName.isEmpty()) {
            page.locator("input[placeholder*='用户名称']").first().fill(userName);
        }
        if (phone != null && !phone.isEmpty()) {
            page.locator("input[placeholder*='手机号码']").first().fill(phone);
        }
        page.locator("button").filter(new Locator.FilterOptions().setHasText("搜索")).first().click();
        page.waitForTimeout(1000);
    }

    @Step("在表格行中点击操作: {rowName} → {buttonText}")
    public void clickRowAction(String rowName, String buttonText) {
        Locator row = tableRow(rowName);
        row.locator("button, a").filter(new Locator.FilterOptions().setHasText(buttonText)).first().click();
        page.waitForTimeout(500);
    }

    @Step("点击操作列中的重置密码: {rowName}")
    public void clickResetPassword(String rowName) {
        clickRowAction(rowName, "重置密码");
    }

    // ==================== Assertions ====================

    @Step("断言用户行可见: {name}")
    public void assertRowVisible(String name) {
        assertThat(tableRow(name)).isVisible();
    }

    @Step("断言Toast消息包含: {text}")
    public void assertToastContains(String text) {
        Locator toast = page.locator(".el-message--success, .el-message--error, .el-message__content").first();
        assertThat(toast).containsText(text);
    }

    @Step("获取Toast消息")
    public String getToastMessage() {
        try {
            Locator toast = page.locator(
                    ".el-message--success, .el-message--error, .el-message--warning, .el-message__content").first();
            if (toast.isVisible()) return toast.textContent();
        } catch (Exception ignored) {}
        return "";
    }
}
