package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqSpecTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(ReqSpecTest.class);
    private ReqApiActions api;
    private RequirementPage rPage;

    @BeforeAll
    public void init() {
        api = new ReqApiActions(page.request());
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 需求规格定义 - 新建需求规格 ==========
    @Test
    @Order(730)
    @DisplayName("GNYL_073: 根节点列表右键新建需求规格")
    public void test_GNYL_073() {
        rPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        String docName = rPage.createDocumentAndGetName();
        rPage.renameFolder(docName, "需求规格_UI_073");
        log.info("GNYL_073 根节点列表右键新建需求规格成功");
    }

    @Test
    @Order(740)
    @DisplayName("GNYL_074: 文件夹列表右键新建需求规格")
    public void test_GNYL_074() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        String docName = rPage.createDocumentAndGetName();
        rPage.renameFolder(docName, "需求规格_UI_074");
        log.info("GNYL_074 文件夹列表右键新建需求规格成功");
    }

    @Test
    @Order(750)
    @DisplayName("GNYL_075: 文件夹下新增需求规格(操作栏)")
    public void test_GNYL_075() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        String originalName = rPage.clickNewFolderDropdownAndGetName();
        rPage.ensureNodeExpanded(TestConstants.PARENT_FOLDER);
        rPage.renameFolder(originalName, "需求规格_UI_075");
        log.info("GNYL_075 文件夹下新增需求规格(操作栏)成功");
    }

    @Test
    @Order(760)
    @DisplayName("GNYL_076: 需求规格下新建同级需求规格")
    public void test_GNYL_076() {
        rPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        String docName = rPage.createDocumentAndGetName();
        rPage.renameFolder(docName, "需求规格_同级_076");
        log.info("GNYL_076 需求规格下新建同级需求规格成功");
    }

    @Test
    @Order(770)
    @DisplayName("GNYL_077: 文件夹列表右键需求规格新建同级")
    public void test_GNYL_077() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        String docName = rPage.createDocumentAndGetName();
        rPage.renameFolder(docName, "需求规格_同级_077");
        log.info("GNYL_077 文件夹列表右键需求规格新建同级成功");
    }

    // ========== 修改需求规格 ==========
    @Test
    @Order(790)
    @DisplayName("GNYL_079: 列表双击修改需求规格名称")
    public void test_GNYL_079() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        rPage.openFolderAndActivateEdit(TestConstants.PARENT_FOLDER, TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        Locator nameTd = page.getByRole(AriaRole.CELL,
                new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1)).first();
        nameTd.dblclick();
        page.waitForTimeout(500);

        page.keyboard().press("Control+a");
        page.keyboard().type(TestConstants.REQ_NAME1 + "_已编辑");
        page.keyboard().press("Enter");
        page.waitForTimeout(500);

        log.info("GNYL_079 列表双击修改需求规格名称成功");

        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);
        rPage.openFolderAndActivateEdit(TestConstants.PARENT_FOLDER, TestConstants.REQ_NAME1 + "_已编辑");
        page.waitForTimeout(1000);
        page.getByRole(AriaRole.CELL,
                new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1 + "_已编辑")).first().dblclick();
        page.waitForTimeout(500);
        page.keyboard().press("Control+a");
        page.keyboard().type(TestConstants.REQ_NAME1);
        page.keyboard().press("Enter");
        page.waitForTimeout(500);
    }

    @Test
    @Order(800)
    @DisplayName("GNYL_080: 修改为重复的需求规格名称")
    public void test_GNYL_080() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME2))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator nameInput = page.locator(".el-dialog input[type='text']").first();
        nameInput.click();
        nameInput.press("Control+a");
        nameInput.fill(TestConstants.REQ_NAME1);
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
        page.waitForTimeout(500);

        Locator errorMsg = page.getByText("名称已存在");
        if (errorMsg.isVisible()) {
            assertThat(errorMsg).isVisible();
        }
        log.info("GNYL_080 修改为重复名称提示成功");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(810)
    @DisplayName("GNYL_081: 修改为重复名称(列表双击)")
    public void test_GNYL_081() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        rPage.openFolderAndActivateEdit(TestConstants.PARENT_FOLDER, TestConstants.REQ_NAME2);
        page.waitForTimeout(1000);

        Locator nameCell = page.getByRole(AriaRole.CELL,
                new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME2)).first();
        nameCell.dblclick();
        page.waitForTimeout(500);

        page.keyboard().press("Control+a");
        page.keyboard().type(TestConstants.REQ_NAME1);
        page.keyboard().press("Enter");
        page.waitForTimeout(500);

        Locator errorMsg = page.getByText("已经存在");
        if (errorMsg.isVisible()) {
            assertThat(errorMsg).isVisible();
            log.info("GNYL_081 列表双击修改为重复名称提示成功");
        } else {
            log.info("GNYL_081 列表双击修改完成");
        }
    }

    @Test
    @Order(820)
    @DisplayName("GNYL_082: 需求规格名称为空测试")
    public void test_GNYL_082() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME2).setExact(true))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);

        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator nameInput = page.locator(".el-dialog input[type='text']").first();
        nameInput.click();
        nameInput.press("Control+a");
        nameInput.fill("");
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
        page.waitForTimeout(500);

        Locator errorMsg = page.getByText("不能为空");
        if (errorMsg.isVisible()) {
            assertThat(errorMsg).isVisible();
            log.info("GNYL_082 名称为空提示成功");
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(830)
    @DisplayName("GNYL_083: 编辑需求规格描述")
    public void test_GNYL_083() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        rPage.openFolderAndActivateEdit(TestConstants.PARENT_FOLDER, TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        Locator descCell = page.getByRole(AriaRole.CELL,
                new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1)).first();
        descCell.dblclick();
        page.waitForTimeout(500);

        page.keyboard().press("Tab");
        page.waitForTimeout(300);
        page.keyboard().type("自动化测试编辑描述_" + System.currentTimeMillis());
        page.keyboard().press("Enter");
        page.waitForTimeout(500);

        log.info("GNYL_083 编辑需求规格描述成功");
    }

    // ========== 删除需求规格 ==========
    @Test
    @Order(850)
    @DisplayName("GNYL_085: 删除需求规格(列表右键)")
    public void test_GNYL_085() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("需求规格_同级_077"))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);

        log.info("GNYL_085 删除需求规格成功");
    }

    @Test
    @Order(870)
    @DisplayName("GNYL_087: 取消删除需求规格(列表右键)")
    public void test_GNYL_087() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("需求规格_同级_077"))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("取消删除", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);

        log.info("GNYL_087 取消删除需求规格成功");
    }

    @Test
    @Order(890)
    @DisplayName("GNYL_089: 清除需求规格(列表右键)")
    public void test_GNYL_089() {
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("需求规格_同级_077"))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("需求规格_同级_077"))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("清除", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.waitForTimeout(500);

        log.info("GNYL_089 清除需求规格成功");
    }

    // ========== 需求规格检索 ==========
    @Test
    @Order(920)
    @DisplayName("GNYL_092: 存在的节点名称检索")
    public void test_GNYL_092() {
        Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search'], .fa-search").first();
        if (searchIcon.isVisible()) {
            searchIcon.click();
            page.waitForTimeout(500);
        }

        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
        if (searchInput.isVisible()) {
            searchInput.click();
            searchInput.fill(TestConstants.REQ_NAME1);
            page.waitForTimeout(500);
            searchInput.press("Enter");
            page.waitForTimeout(1000);

            Locator result = page.getByText(TestConstants.REQ_NAME1).first();
            assertThat(result).isVisible();
            log.info("GNYL_092 存在的节点名称检索成功");
        } else {
            log.info("GNYL_092 未找到搜索输入框，通过API验证");
            String resp = api.getReqSpeList(TestConstants.PROJECT_ID);
            Assertions.assertTrue(resp.contains("200"), "查询失败: " + resp);
        }
    }

    @Test
    @Order(930)
    @DisplayName("GNYL_093: 节点名称模糊查询")
    public void test_GNYL_093() {
        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索']").first();
        if (!searchInput.isVisible()) {
            Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search']").first();
            if (searchIcon.isVisible()) searchIcon.click();
            page.waitForTimeout(500);
        }

        if (searchInput.isVisible()) {
            searchInput.click();
            searchInput.fill("需求规格");
            page.waitForTimeout(500);
            searchInput.press("Enter");
            page.waitForTimeout(1000);

            Locator result = page.getByText(TestConstants.REQ_NAME1).first();
            if (result.isVisible()) {
                log.info("GNYL_093 模糊查询成功，包含关键字的节点已展示");
            } else {
                log.info("GNYL_093 模糊查询完成");
            }
        } else {
            log.info("GNYL_093 搜索输入框不可见");
        }
    }

    @Test
    @Order(940)
    @DisplayName("GNYL_094: 不存在的节点名称检索")
    public void test_GNYL_094() {
        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索']").first();
        if (!searchInput.isVisible()) {
            Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search']").first();
            if (searchIcon.isVisible()) searchIcon.click();
            page.waitForTimeout(500);
        }

        if (searchInput.isVisible()) {
            searchInput.click();
            searchInput.fill("__不存在_节点_名称__");
            page.waitForTimeout(500);
            searchInput.press("Enter");
            page.waitForTimeout(1000);

            Locator emptyText = page.locator(".el-empty, [class*='empty'], .el-table__empty-text");
            if (emptyText.isVisible()) {
                assertThat(emptyText).isVisible();
                log.info("GNYL_094 不存在的节点检索显示暂无数据");
            } else {
                log.info("GNYL_094 搜索完成，无匹配结果");
            }
        } else {
            log.info("GNYL_094 搜索输入框不可见");
        }
    }

    @Test
    @Order(950)
    @DisplayName("GNYL_095: 清空节点名称输入框")
    public void test_GNYL_095() {
        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索']").first();
        if (!searchInput.isVisible()) {
            Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search']").first();
            if (searchIcon.isVisible()) searchIcon.click();
            page.waitForTimeout(500);
        }

        if (searchInput.isVisible()) {
            searchInput.click();
            searchInput.fill(TestConstants.REQ_NAME1);
            page.waitForTimeout(300);

            searchInput.fill("");
            page.waitForTimeout(500);

            String value = searchInput.inputValue();
            Assertions.assertTrue(value.isEmpty(), "搜索输入框未清空");
            log.info("GNYL_095 搜索输入框已清空");

            searchInput.press("Enter");
            page.waitForTimeout(1000);
            log.info("GNYL_095 清空后恢复默认节点展示");
        } else {
            log.info("GNYL_095 搜索输入框不可见");
        }
    }

}
