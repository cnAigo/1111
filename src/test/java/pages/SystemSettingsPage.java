package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for 系统设置 (System Settings) — covers:
 * 视图管理, 自定义属性, 收藏, 项目人员管理.
 */
public class SystemSettingsPage {

    private final Page page;

    public SystemSettingsPage(Page page) {
        this.page = page;
    }

    // ==================== Navigation ====================

    @Step("点击侧边栏菜单: {menuName}")
    public void clickSidebarMenu(String menuName) {
        Locator menu = page.locator("[role='menuitem'], .el-menu-item, li")
                .filter(new Locator.FilterOptions().setHasText(menuName)).first();
        menu.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        menu.click();
        page.waitForTimeout(1500);
    }

    // ==================== View Management (视图管理) ====================

    @Step("点击新建视图按钮")
    public void clickNewView() {
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("新建视图|新增视图|添加视图")))
                .first().click();
        page.waitForTimeout(500);
    }

    @Step("填写视图名称: {name}")
    public void fillViewName(String name) {
        page.locator(".el-dialog:visible input[type='text']").first().fill(name);
    }

    @Step("填写视图描述: {desc}")
    public void fillViewDescription(String desc) {
        page.locator(".el-dialog:visible textarea").first().fill(desc);
    }

    @Step("点击确定")
    public void clickConfirm() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确"))).first().click();
        page.waitForTimeout(800);
    }

    @Step("在视图列表中删除视图: {viewName}")
    public void deleteView(String viewName) {
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(viewName))
                .locator("button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("删除|移除")))
                .first().click();
        page.waitForTimeout(300);
    }

    // ==================== Custom Attributes (自定义属性) ====================

    @Step("点击新建属性按钮")
    public void clickNewAttribute() {
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("新增|新建|添加")))
                .first().click();
        page.waitForTimeout(500);
    }

    @Step("填写属性英文名: {nameEn}")
    public void fillAttrNameEn(String nameEn) {
        fillDialogField("英文名", nameEn);
    }

    @Step("填写属性名称: {name}")
    public void fillAttrName(String name) {
        fillDialogField("名称", name);
    }

    @Step("选择属性类型: {type}")
    public void selectAttrType(String type) {
        page.locator(".el-dialog:visible .el-form-item")
                .filter(new Locator.FilterOptions().setHasText("类型"))
                .locator("input, .el-select").first().click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(type)).click();
        page.waitForTimeout(300);
    }

    @Step("点击发布按钮: {attrName}")
    public void publishAttribute(String attrName) {
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(attrName))
                .locator("button")
                .filter(new Locator.FilterOptions().setHasText("发布"))
                .first().click();
        page.waitForTimeout(500);
    }

    private void fillDialogField(String labelText, String value) {
        Locator input = page.locator(".el-dialog:visible .el-form-item")
                .filter(new Locator.FilterOptions().setHasText(labelText))
                .locator("input, textarea").first();
        input.click();
        input.press("Control+a");
        input.fill(value);
    }

    // ==================== Favorites (收藏) ====================

    @Step("右键收藏节点: {nodeName}")
    public void rightClickAndFavorite(String nodeName) {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(nodeName))
                .locator("div").first()
                .click(new Locator.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT));
        page.waitForTimeout(300);

        Locator favItem = page.locator("span, li").filter(new Locator.FilterOptions().setHasText("收藏")).first();
        if (favItem.isVisible()) favItem.click();
        page.waitForTimeout(500);
    }

    @Step("点击收藏菜单查看收藏列表")
    public void navigateToFavorites() {
        Locator favMenu = page.locator("span, div, li, [role='menuitem']")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("收藏|favorite|星标")))
                .first();
        if (favMenu.isVisible()) {
            favMenu.click();
            page.waitForTimeout(1000);
        }
    }

    @Step("取消收藏: {itemName}")
    public void removeFavorite(String itemName) {
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(itemName))
                .locator("button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("取消|删除|移除")))
                .first().click();
        page.waitForTimeout(500);
    }

    @Step("点击确认删除")
    public void clickDeleteConfirm() {
        Locator confirm = page.locator("button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确定|确 定")));
        if (confirm.isVisible()) confirm.click();
        page.waitForTimeout(500);
    }

    // ==================== Project Person (项目人员管理) ====================

    @Step("搜索用户: {keyword}")
    public void searchPerson(String keyword) {
        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='用户'], input[placeholder*='姓名']").first();
        if (searchInput.isVisible()) {
            searchInput.fill(keyword);
            searchInput.press("Enter");
            page.waitForTimeout(800);
        }
    }

    @Step("勾选用户: {userName}")
    public void checkUser(String userName) {
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(userName))
                .locator("input[type='checkbox'], .el-checkbox").first().click();
        page.waitForTimeout(300);
    }

    @Step("点击分配按钮")
    public void clickAssign() {
        page.locator("button").filter(new Locator.FilterOptions().setHasText(Pattern.compile("分配|确定|保存")))
                .first().click();
        page.waitForTimeout(800);
    }

    @Step("展开部门树")
    public void expandDeptTree() {
        Locator tree = page.locator(".el-tree, [class*='tree'], [class*='dept']").first();
        if (tree.isVisible()) {
            tree.locator(".el-tree-node__expand-icon").first().click();
            page.waitForTimeout(500);
        }
    }

    // ==================== Assertions ====================

    @Step("断言Toast包含: {text}")
    public void assertToastContains(String text) {
        Locator toast = page.locator(".el-message--success, .el-message--error, .el-message__content").first();
        assertThat(toast).containsText(text);
    }

    @Step("断言行可见: {name}")
    public void assertRowVisible(String name) {
        assertThat(page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(name)).first()).isVisible();
    }
}
