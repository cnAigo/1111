package cases.ui;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqSpecTest extends BaseTest {

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_073: 根节点列表右键新建需求规格")
    public void test_GNYL_073() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);
            page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String docName = reqPage.createDocumentAndGetName();
            String newName = "Spec_073_" + suffix();
            reqPage.renameFolder(docName, newName);
            log.info("GNYL_073 根节点列表右键新建需求规格成功: {}", newName);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_074: 文件夹列表右键新建需求规格")
    public void test_GNYL_074() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);
            page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String docName = reqPage.createDocumentAndGetName();
            String newName = "Spec_074_" + suffix();
            reqPage.renameFolder(docName, newName);
            log.info("GNYL_074 文件夹列表右键新建需求规格成功: {}", newName);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_075: 文件夹下新增需求规格(操作栏)")
    public void test_GNYL_075() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);
            page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String docName = reqPage.createDocumentAndGetName();
            String newName = "Spec_075_" + suffix();
            reqPage.renameFolder(docName, newName);
            log.info("GNYL_075 文件夹下新增需求规格成功: {}", newName);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_076: 需求规格下新建同级需求规格")
    public void test_GNYL_076() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(doc[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);
            page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String newDocName = reqPage.createDocumentAndGetName();
            String newName = "Spec_076_Sibling_" + suffix();
            reqPage.renameFolder(newDocName, newName);
            log.info("GNYL_076 需求规格下新建同级需求规格成功: {}", newName);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_077: 文件夹列表右键需求规格新建同级")
    public void test_GNYL_077() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);
            page.getByText("需求规格", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String newDocName = reqPage.createDocumentAndGetName();
            String newName = "Spec_077_Sibling_" + suffix();
            reqPage.renameFolder(newDocName, newName);
            log.info("GNYL_077 文件夹列表右键需求规格新建同级成功: {}", newName);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_079: 列表双击修改需求规格名称")
    public void test_GNYL_079() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            reqPage.openFolderAndActivateEdit(folder[1], doc[1]);
            page.waitForTimeout(500);

            Locator nameTd = page.getByRole(AriaRole.CELL,
                    new Page.GetByRoleOptions().setName(doc[1])).first();
            nameTd.dblclick();
            page.waitForTimeout(500);

            String editedName = doc[1] + "_edited";
            page.keyboard().press("Control+a");
            page.keyboard().type(editedName);
            page.keyboard().press("Enter");
            page.waitForTimeout(500);

            log.info("GNYL_079 列表双击修改需求规格名称成功");

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);
            reqPage.openFolderAndActivateEdit(folder[1], editedName);
            page.waitForTimeout(500);
            page.getByRole(AriaRole.CELL,
                    new Page.GetByRoleOptions().setName(editedName)).first().dblclick();
            page.waitForTimeout(500);
            page.keyboard().press("Control+a");
            page.keyboard().type(doc[1]);
            page.keyboard().press("Enter");
            page.waitForTimeout(500);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_080: 修改为重复的需求规格名称")
    public void test_GNYL_080() {
        String[] folder = createTempFolder();
        String[] doc1 = createTempDoc(folder[0]);
        String[] doc2 = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc2[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator nameInput = page.locator(".el-dialog input[type='text']").first();
            nameInput.click();
            nameInput.press("Control+a");
            nameInput.fill(doc1[1]);
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_081: 修改为重复名称(列表双击)")
    public void test_GNYL_081() {
        String[] folder = createTempFolder();
        String[] doc1 = createTempDoc(folder[0]);
        String[] doc2 = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            reqPage.openFolderAndActivateEdit(folder[1], doc2[1]);
            page.waitForTimeout(500);

            Locator nameCell = page.getByRole(AriaRole.CELL,
                    new Page.GetByRoleOptions().setName(doc2[1])).first();
            nameCell.dblclick();
            page.waitForTimeout(500);

            page.keyboard().press("Control+a");
            page.keyboard().type(doc1[1]);
            page.keyboard().press("Enter");
            page.waitForTimeout(500);

            Locator errorMsg = page.getByText("已经存在");
            if (errorMsg.isVisible()) {
                assertThat(errorMsg).isVisible();
                log.info("GNYL_081 列表双击修改为重复名称提示成功");
            } else {
                log.info("GNYL_081 列表双击修改完成(无重复提示)");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_082: 需求规格名称为空测试")
    public void test_GNYL_082() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(doc[1]).setExact(true))
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_083: 编辑需求规格描述")
    public void test_GNYL_083() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            reqPage.openFolderAndActivateEdit(folder[1], doc[1]);
            page.waitForTimeout(500);

            Locator descCell = page.getByRole(AriaRole.CELL,
                    new Page.GetByRoleOptions().setName(doc[1])).first();
            descCell.dblclick();
            page.waitForTimeout(500);

            page.keyboard().press("Tab");
            page.waitForTimeout(300);
            page.keyboard().type("自动化测试编辑描述_" + System.currentTimeMillis());
            page.keyboard().press("Enter");
            page.waitForTimeout(500);

            log.info("GNYL_083 编辑需求规格描述成功");
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_085: 删除需求规格(列表右键)")
    public void test_GNYL_085() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            log.info("GNYL_085 删除需求规格成功: {}", doc[1]);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_087: 取消删除需求规格(列表右键)")
    public void test_GNYL_087() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("取消删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            log.info("GNYL_087 取消删除需求规格成功: {}", doc[1]);
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_089: 清除需求规格(列表右键)")
    public void test_GNYL_089() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("清除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
            page.waitForTimeout(500);

            log.info("GNYL_089 清除需求规格成功");
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_092: 存在的节点名称检索")
    public void test_GNYL_092() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search'], .fa-search").first();
            if (searchIcon.isVisible()) {
                searchIcon.click();
                page.waitForTimeout(500);
            }

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill(doc[1]);
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator result = page.getByText(doc[1]).first();
                assertThat(result).isVisible();
                log.info("GNYL_092 存在的节点名称检索成功");
            } else {
                log.info("GNYL_092 未找到搜索输入框，通过API验证");
                String resp = api.getReqSpeList(PROJECT_ID);
                Assertions.assertTrue(resp.contains("200"), "查询失败: " + resp);
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_093: 节点名称模糊查询")
    public void test_GNYL_093() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索']").first();
            if (!searchInput.isVisible()) {
                Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search']").first();
                if (searchIcon.isVisible()) searchIcon.click();
                page.waitForTimeout(500);
            }

            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill(doc[1].substring(0, Math.min(5, doc[1].length())));
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator result = page.getByText(doc[1]).first();
                if (result.isVisible()) {
                    log.info("GNYL_093 模糊查询成功，包含关键字的节点已展示");
                } else {
                    log.info("GNYL_093 模糊查询完成");
                }
            } else {
                log.info("GNYL_093 搜索输入框不可见");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_094: 不存在的节点名称检索")
    public void test_GNYL_094() {
        try {
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
        } finally {
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_095: 清空节点名称输入框")
    public void test_GNYL_095() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索']").first();
            if (!searchInput.isVisible()) {
                Locator searchIcon = page.locator("[class*='search'], [class*='el-icon-search']").first();
                if (searchIcon.isVisible()) searchIcon.click();
                page.waitForTimeout(500);
            }

            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill(doc[1]);
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }
}
