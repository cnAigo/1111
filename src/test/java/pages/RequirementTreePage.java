package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import config.TestConstants;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for the left-side requirement tree panel.
 */
public class RequirementTreePage {

    private final Page page;

    public RequirementTreePage(Page page) {
        this.page = page;
    }

    @Step("刷新页面树结构")
    public void refreshTree() {
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Step("等待树节点可见: {nodeName}")
    public void waitForTreeNodeVisible(String nodeName) {
        page.locator(".el-tree-node:visible")
                .filter(new Locator.FilterOptions().setHasText(nodeName))
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000));
    }

    @Step("右键点击树节点: {nodeName}")
    public void rightClickTreeNode(String nodeName) {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(nodeName))
                .locator("div").first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
    }

    @Step("双击树节点进入列表: {nodeName}")
    public void doubleClickTreeNode(String nodeName) {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(nodeName)).first().dblclick();
    }

    @Step("点击上下文菜单项: {menuName}")
    public void clickContextMenu(String menuName) {
        page.getByText(menuName, new Page.GetByTextOptions().setExact(true))
                .last().click();
    }

    @Step("确保树节点已展开: {nodeName}")
    public void ensureNodeExpanded(String nodeName) {
        Locator node = page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(nodeName)).locator("div").first();
        Locator expandArrow = node.locator(".el-tree-node__expand-icon").first();
        String cls = expandArrow.getAttribute("class");
        if (cls != null && !cls.contains("expanded") && !cls.contains("is-leaf")) {
            expandArrow.click();
        }
    }

    @Step("在对话框中填写输入: {text}")
    public void fillDialogInput(String text) {
        Locator input = page.locator(".el-dialog:visible input[type='text']").first();
        if (input.isVisible()) {
            input.click();
            input.press("Control+a");
            input.fill(text);
        }
    }

    @Step("点击对话框确定按钮")
    public void clickDialogConfirm() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确")))
                .first().click();
        page.waitForTimeout(500);
    }

    @Step("获取页面Toast消息")
    public String getToastMessage() {
        try {
            Locator toast = page.locator(".el-message--error, .el-message--success, .el-message__content").first();
            if (toast.isVisible()) return toast.textContent();
        } catch (Exception ignored) {}
        return "";
    }
}
