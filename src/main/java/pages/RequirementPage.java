package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import config.TestConfig;
import config.TestConstants;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RequirementPage {

    private static final String API_ADD_FOLDER = "/erm/add/addReqSpeFolder";

    private final Page page;

    public RequirementPage(Page page) {
        this.page = page;
    }

    public void waitForTreeNodeVisible(String nodeName) {
        page.locator(".el-tree-node:visible")
                .filter(new Locator.FilterOptions().setHasText(nodeName))
                .first()
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000));
    }

    public void rightClickTreeNode(String nodeName) {
        Locator node = page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(nodeName)).locator("div").first();
        node.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
    }

    public void doubleClickTreeNode(String nodeName) {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(nodeName)).first().dblclick();
    }

    public void clickContextMenu(String menuName) {
        page.locator("span")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + menuName + "$")))
                .click();
    }

    public void ensureNodeExpanded(String nodeName) {
        Locator node = page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(nodeName)).locator("div").first();
        Locator expandArrow = node.locator(".el-tree-node__expand-icon").first();
        String cls = expandArrow.getAttribute("class");
        if (cls != null && !cls.contains("expanded") && !cls.contains("is-leaf")) {
            expandArrow.click();
        }
    }

    public String[] createFolderAndGetDetails() {
        Response response = page.waitForResponse(
                res -> res.url().contains(API_ADD_FOLDER) && res.status() == 200,
                () -> page.getByText("文件夹", new Page.GetByTextOptions().setExact(true)).click()
        );

        String body = response.text();
        String title = extractJsonValue(body, "title");
        String objectId = extractJsonValue(body, "objectId");

        return new String[]{title, objectId};
    }

    public String createDocumentAndGetName() {
        Response response = page.waitForResponse(
                res -> res.url().contains(API_ADD_FOLDER) && res.status() == 200,
                () -> page.getByText("文件夹", new Page.GetByTextOptions().setExact(true)).click()
        );

        return extractJsonValue(response.text(), "title");
    }

    public String clickNewFolderDropdownAndGetName() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("新建").setExact(true)).click();

        Locator folderOption = page.getByText("新增文件夹").last();
        folderOption.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        Response response = page.waitForResponse(
                res -> res.url().contains(API_ADD_FOLDER) && res.status() == 200,
                () -> folderOption.click()
        );

        return extractJsonValue(response.text(), "title");
    }

    public void renameFolder(String originalName, String newName) {
        Locator node = page.locator(".el-tree-node")
                .filter(new Locator.FilterOptions().setHasText(originalName))
                .first();
        node.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(15000));

        Locator focusedInput = page.locator("input:focus");
        if (focusedInput.count() > 0 && focusedInput.isVisible()) {
            focusedInput.press("Control+a");
            focusedInput.fill(newName);
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(TestConstants.ROOT_NODE))
                    .locator("div").first().click();
            page.waitForTimeout(500);
        } else {
            node.locator(".el-tree-node__label, .custom-tree-node").first().click();
            page.waitForTimeout(300);
            node.locator(".el-tree-node__label, .custom-tree-node").first().click();
            page.waitForTimeout(300);

            Locator editInput = page.locator("input:focus");
            editInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            editInput.press("Control+a");
            editInput.fill(newName);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(TestConstants.ROOT_NODE))
                    .locator("div").first().click();
            page.waitForTimeout(500);
        }

        Locator renamed = page.locator(".el-tree-node")
                .filter(new Locator.FilterOptions().setHasText(newName)).first();
        renamed.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        assertThat(renamed).isVisible();
    }

    public void safeDeleteFolder(String folderName) {
        Locator target = page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(folderName).setExact(true));

        if (target.count() == 0) return;
        target.scrollIntoViewIfNeeded();

        target.locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).last()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).last().click();
        page.mouse().click(0, 0);
        page.waitForTimeout(500);

        target.locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.getByText("清除", new Page.GetByTextOptions().setExact(true)).last()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        page.getByText("清除", new Page.GetByTextOptions().setExact(true)).last().click();

        try {
            Locator confirm = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                    .filter(new Locator.FilterOptions().setHasText("确定")).first();
            confirm.waitFor(new Locator.WaitForOptions().setTimeout(3000));
            confirm.focus();
            confirm.press("Space");
        } catch (TimeoutError e) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定"))
                    .waitFor(new Locator.WaitForOptions().setTimeout(3000));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        }

        try {
            target.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
        } catch (Exception ignored) {}
        assertThat(target).isHidden();
    }

    public void openFolderAndActivateEdit(String parentName, String targetName) {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(parentName).setExact(true)).locator("img").click();

        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(parentName).setExact(true)).dblclick();

        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(targetName))
                .locator(".cell-title").first().click();

        page.getByText(targetName).dblclick();
    }

    public void activateRenameInput(String nodeName) {
        Locator node = page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(nodeName).setExact(true))
                .locator("div").first();
        node.click();
        node.getByText(nodeName).click();
    }

    public String fillRenameAndSave(String newName) {
        Locator editInput = page.locator("input:focus");
        editInput.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        editInput.press("Control+a");
        editInput.fill(newName);

        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName("需求（根节点）"))
                .locator("div").first().click();
        page.waitForTimeout(1000);

        try {
            Locator toast = page.locator(".el-message--error, .el-message__content")
                    .first();
            if (toast.isVisible()) {
                return toast.textContent();
            }
        } catch (Exception ignored) {}

        return "";
    }

    public void navigateToAttributeList() {
        page.navigate(TestConfig.BASE_URL + "/#/SystemManagement");
        page.waitForTimeout(2000);
        page.getByRole(AriaRole.MENUITEM,
                new Page.GetByRoleOptions().setName("合作区管理")).click();
        page.waitForTimeout(1000);
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("test1"))
                .getByRole(AriaRole.BUTTON).nth(3).click();
        page.waitForTimeout(1000);
    }

    public void openAddDialog() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("新增")).click();
        page.waitForTimeout(500);
    }

    public void selectEnumType() {
        page.getByLabel("添加参数").locator("div")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^请选择$")))
                .nth(3).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName("枚举")).click();
        page.waitForTimeout(500);
    }

    public void closeDialog() {
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(300);
    }

    private String extractJsonValue(String json, String fieldName) {
        try {
            int start = json.indexOf("\"" + fieldName + "\":\"") + fieldName.length() + 4;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }
}
