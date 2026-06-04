package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for the right-side workspace panel (list/table + dialogs).
 */
public class RequirementWorkspacePage {

    private final Page page;

    public RequirementWorkspacePage(Page page) {
        this.page = page;
    }

    // ==================== Row Actions ====================

    @Step("等待列表行可见: {rowName}")
    public boolean waitForRowVisible(String rowName) {
        for (int attempt = 0; attempt < 10; attempt++) {
            Locator row = page.getByRole(AriaRole.ROW,
                    new Page.GetByRoleOptions().setName(rowName)).first();
            if (row.count() > 0) {
                try {
                    row.waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE).setTimeout(3000));
                    return true;
                } catch (TimeoutError e) {
                    page.waitForTimeout(500);
                }
            }
            page.waitForTimeout(500);
        }
        return false;
    }

    @Step("右键点击列表行: {rowName}")
    public void rightClickRow(String rowName) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Locator row = page.getByRole(AriaRole.ROW,
                    new Page.GetByRoleOptions().setName(rowName)).first();
            if (row.count() == 0) { page.waitForTimeout(500); continue; }
            try {
                row.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
                page.waitForTimeout(300);
                return;
            } catch (TimeoutError e) { page.waitForTimeout(500); }
        }
    }

    @Step("双击列表行进入子目录: {rowName}")
    public void doubleClickRow(String rowName) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Locator row = page.getByRole(AriaRole.ROW,
                    new Page.GetByRoleOptions().setName(rowName)).first();
            if (row.count() == 0) { page.waitForTimeout(500); continue; }
            row.dblclick();
            page.waitForTimeout(800);
            return;
        }
    }

    // ==================== Context Menu ====================

    @Step("点击上下文菜单项: {itemName}")
    public void clickContextMenuItem(String itemName) {
        page.getByText(itemName, new Page.GetByTextOptions().setExact(true))
                .last().click();
        page.waitForTimeout(300);
    }

    @Step("通过右键菜单执行操作: 右键{rowName} → {menuItems}")
    public void rightClickAndSelect(String rowName, String... menuItems) {
        rightClickRow(rowName);
        for (String item : menuItems) {
            clickContextMenuItem(item);
        }
    }

    @Step("检查上下文菜单项是否存在: {itemName}")
    public boolean hasContextMenuItem(String itemName) {
        try {
            return page.getByText(itemName, new Page.GetByTextOptions().setExact(true))
                    .last().isVisible();
        } catch (Exception ignored) { return false; }
    }

    // ==================== Dialog ====================

    @Step("在属性对话框中填写名称: {text}")
    public void fillDialogNameInput(String text) {
        Locator input = page.locator(".el-dialog:visible input[type='text']").first();
        input.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        input.click();
        input.press("Control+a");
        input.fill(text);
    }

    @Step("点击对话框确定按钮")
    public void clickConfirmButton() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确")))
                .first().click();
        page.waitForTimeout(500);
    }

    @Step("点击确认删除弹窗按钮")
    public void clickDeleteConfirm() {
        try {
            Locator confirm = page.locator(".el-message-box__btns button, button")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确")));
            if (confirm.isVisible()) confirm.click();
        } catch (Exception ignored) {}
        page.waitForTimeout(500);
    }

    // ==================== Search ====================

    @Step("查找搜索输入框")
    private Locator findSearch() {
        String[] selectors = {
                "input[placeholder*='搜索']", "input[placeholder*='检索']",
                "input[placeholder*='查找']", "input[type='text'][placeholder*='名称']",
        };
        for (String sel : selectors) {
            Locator loc = page.locator(sel).first();
            try { if (loc.count() > 0 && loc.isVisible()) return loc; }
            catch (Exception ignored) {}
        }
        return null;
    }

    @Step("搜索关键词: {keyword}")
    public void searchFor(String keyword) {
        Locator searchInput = findSearch();
        if (searchInput != null) {
            searchInput.click();
            searchInput.fill(keyword);
            searchInput.press("Enter");
            page.waitForTimeout(1000);
        }
    }

    // ==================== Action Bar ====================

    @Step("点击操作栏新建按钮")
    public void clickNewButton() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("新建").setExact(true)).click();
        page.waitForTimeout(300);
    }

    @Step("点击下拉菜单: 新增文件夹")
    public void clickNewFolderDropdownOption() {
        page.getByText("新增文件夹").last()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        page.getByText("新增文件夹").last().click();
        page.waitForTimeout(500);
    }

    // ==================== Assertions ====================

    @Step("断言行可见: {rowName}")
    public void assertRowVisible(String rowName) {
        Locator row = page.getByRole(AriaRole.ROW,
                new Page.GetByRoleOptions().setName(rowName)).first();
        assertThat(row).isVisible();
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
