package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for 合作区管理 (Cooperation Area Management) page.
 * Accessed via SystemManagement sidebar menu.
 */
public class CooperationAreaPage {

    private final Page page;

    public CooperationAreaPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    @Step("点击侧边栏菜单: 合作区管理")
    public void navigateToCoopArea() {
        // 先展开"系统管理"父菜单（如果收起）
        expandSidebarParent("系统管理");
        // 再点击子菜单项
        clickSidebarItem("合作区管理");
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
        return page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("新增|添加"))).first();
    }

    private Locator editButton() {
        return page.locator("button").filter(new Locator.FilterOptions().setHasText("修改")).first();
    }

    private Locator deleteButton() {
        return page.locator("button").filter(new Locator.FilterOptions().setHasText("删除")).first();
    }

    private Locator dialog() {
        return page.locator(".el-dialog:visible").first();
    }

    private Locator dialogInput(String labelText) {
        return dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText(labelText))
                .locator("input").first();
    }

    private Locator tableRow(String name) {
        return page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(name)).first();
    }

    // ==================== Actions ====================

    @Step("点击新增按钮并打开对话框")
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

    @Step("填写合作区名称: {name}")
    public void fillName(String name) {
        dialogInput("合作区名称").fill(name);
    }

    @Step("填写合作区编码: {code}")
    public void fillCode(String code) {
        dialogInput("合作区编码").fill(code);
    }

    @Step("选择密级: {level}")
    public void selectSecurityLevel(String level) {
        dialog().locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("密级"))
                .locator("input, .el-select").first().click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(level)).click();
        page.waitForTimeout(300);
    }

    @Step("点击对话框确定按钮")
    public void clickConfirm() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确"))).first().click();
        page.waitForTimeout(800);
    }

    @Step("点击确认删除")
    public void clickDeleteConfirm() {
        Locator confirm = page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("确定|确 定")));
        if (confirm.isVisible()) confirm.click();
        page.waitForTimeout(500);
    }

    @Step("在搜索框搜索: {keyword}")
    public void search(String keyword) {
        Locator searchInput = page.locator("input[placeholder*='搜索']").first();
        searchInput.fill(keyword);
        page.locator("button").filter(new Locator.FilterOptions().setHasText("搜索")).first().click();
        page.waitForTimeout(1000);
    }

    @Step("点击表格行: {rowName}")
    public void clickRow(String rowName) {
        tableRow(rowName).click();
        page.waitForTimeout(300);
    }

    @Step("点击行操作按钮: 在{rowName}行点击{buttonText}")
    public void clickRowAction(String rowName, String buttonText) {
        tableRow(rowName).locator("button").filter(new Locator.FilterOptions().setHasText(buttonText)).first().click();
        page.waitForTimeout(500);
    }

    // ==================== Assertions ====================

    @Step("断言合作区行可见: {name}")
    public void assertRowVisible(String name) {
        assertThat(tableRow(name)).isVisible();
    }

    @Step("断言Toast消息包含: {text}")
    public void assertToastContains(String text) {
        Locator toast = page.locator(".el-message--success, .el-message--error, .el-message__content").first();
        assertThat(toast).containsText(text);
    }

    @Step("等待Toast消息出现")
    public String waitForToast() {
        Locator toast = page.locator(".el-message, .el-message--success, .el-message--error").first();
        toast.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        return toast.textContent();
    }
}
