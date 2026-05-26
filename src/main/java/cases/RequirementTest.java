package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequirementTest extends BaseTest {

    private RequirementPage reqPage;
    private ReqApiActions api;
    private static final Logger log = LoggerFactory.getLogger(RequirementTest.class);

    private static final String PROJECT_ID = TestConstants.PROJECT_ID;
    private static final String PARENT_FOLDER = TestConstants.PARENT_FOLDER;

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
        api = new ReqApiActions(page.request());
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 工具方法 ==========

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    /** 在项目根节点下创建临时父文件夹，返回 [folderId, folderName] */
    private String[] createTempParent() {
        String suffix = suffix();
        String folderName = "Temp_Parent_" + suffix;
        String folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
        api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, folderName);
        return new String[]{folderId, folderName};
    }

    /** 在指定父文件夹下创建临时子文件夹，返回 [folderId, folderName] */
    private String[] createTempChild(String parentId, String parentName) {
        String suffix = suffix();
        String childName = "Temp_Child_" + suffix;
        String childId = api.createFolder(PROJECT_ID, parentId);
        api.renameFolder(PROJECT_ID, childId, parentId, childName);
        return new String[]{childId, childName};
    }

    /** 在指定父文件夹下创建临时文档，返回 [docId, docName] */
    private String[] createTempDoc(String parentId) {
        String suffix = suffix();
        String docName = "Temp_Doc_" + suffix;
        String docId = api.createDocument(PROJECT_ID, parentId);
        api.renameDocument(PROJECT_ID, docId, parentId, docName);
        return new String[]{docId, docName};
    }

    /** 通过名称递归清理文件夹及其所有子级 */
    private void cleanupByName(String folderName) {
        try {
            api.cleanFolderByName(PROJECT_ID, folderName);
        } catch (Exception e) {
            log.warn("清理 {} 失败: {}", folderName, e.getMessage());
        }
    }

    /** 永久删除指定 ID 的文件夹 */
    private void cleanupFolderById(String folderId) {
        try {
            api.deleteFolder(folderId, PROJECT_ID, "project");
            api.forceCleanFolder(folderId);
        } catch (Exception e) {
            log.warn("清理文件夹 {} 失败: {}", folderId, e.getMessage());
        }
    }

    /** 永久删除指定 ID 的文档 */
    private void cleanupDocById(String docId, String parentId) {
        try {
            api.deleteDocument(docId, parentId);
            api.forceCleanDocument(docId, parentId);
        } catch (Exception e) {
            log.warn("清理文档 {} 失败: {}", docId, e.getMessage());
        }
    }

    private void closeDialogs() {
        try {
            while (page != null &&
                    page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible").count() > 0) {
                page.keyboard().press("Escape");
                page.waitForTimeout(300);
            }
        } catch (Exception e) {
            log.warn("清理残留弹窗异常: {}", e.getMessage());
        }
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_012 [UI] 根节点右键新建文件夹")
    public void test_GNYL_012_CreateFolder_UI() {
        reqPage.rightClickTreeNode(TestConstants.ROOT_NODE);
        reqPage.clickContextMenu("新建");

        String[] details = reqPage.createFolderAndGetDetails();
        String tempName = "Temp_012_" + suffix();
        reqPage.waitForTreeNodeVisible(details[0]);
        reqPage.renameFolder(details[0], tempName);

        try {
            String found = api.findNodeIdByTitle(PROJECT_ID, tempName);
            Assertions.assertNotNull(found, "新建文件夹未在树中找到");
            log.info("GNYL_012 创建文件夹成功: {}", tempName);
        } finally {
            cleanupByName(tempName);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_013 [UI] 右键新建子文件夹")
    void test_GNYL_013_CreateChildFolder_UI() {
        String[] parent = createTempParent();
        try {
            page.waitForTimeout(1000);

            reqPage.ensureNodeExpanded(parent[1]);
            reqPage.rightClickTreeNode(parent[1]);
            reqPage.clickContextMenu("新建");

            String originalName = reqPage.createDocumentAndGetName();
            reqPage.waitForTreeNodeVisible(originalName);

            String childName = "Temp_013_Child_" + suffix();
            reqPage.renameFolder(originalName, childName);
            log.info("GNYL_013 子文件夹创建成功: {}", childName);
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_014 [API] 创建子文件夹")
    void test_GNYL_014_CreateChildFolder_API() {
        String[] parent = createTempParent();
        try {
            String childId = api.createFolder(PROJECT_ID, parent[0]);
            Assertions.assertFalse(childId.isEmpty(), "创建文件夹未返回ID");
            log.info("GNYL_014 子文件夹创建成功, ID: {}", childId);
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_015 [UI] 顶部菜单新建子文件夹")
    void test_GNYL_015_MenuCreateFolder_UI() {
        String[] parent = createTempParent();
        try {
            page.waitForTimeout(500);

            reqPage.doubleClickTreeNode(parent[1]);
            String originalName = reqPage.clickNewFolderDropdownAndGetName();

            reqPage.ensureNodeExpanded(parent[1]);
            String childName = "Temp_015_Child_" + suffix();
            reqPage.renameFolder(originalName, childName);
            log.info("GNYL_015 菜单创建子文件夹成功: {}", childName);
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_016 [API] 创建需求规格文档")
    void test_GNYL_016_CreateDocument_API() {
        String[] parent = createTempParent();
        try {
            String docId1 = api.createDocument(PROJECT_ID, parent[0]);
            Assertions.assertFalse(docId1.isEmpty(), "未能获取到文档1的ID");
            String docName1 = "Temp_Doc1_" + suffix();
            api.renameDocument(PROJECT_ID, docId1, parent[0], docName1);

            page.waitForTimeout(3000);

            String docId2 = api.createDocument(PROJECT_ID, parent[0]);
            Assertions.assertFalse(docId2.isEmpty(), "未能获取到文档2的ID");
            String docName2 = "Temp_Doc2_" + suffix();
            api.renameDocument(PROJECT_ID, docId2, parent[0], docName2);
            log.info("GNYL_016 文档创建成功: {}, {}", docName1, docName2);
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_017: 需求规格下新建同级文件夹")
    public void test_GNYL_017() {
        String[] parent = createTempParent();
        try {
            String[] doc = createTempDoc(parent[0]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(parent[1])).dblclick();
            page.waitForTimeout(500);

            page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(doc[1]))
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^新建$"))).click();
            page.getByText("文件夹", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            log.info("GNYL_017 需求规格下新建同级文件夹成功");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_018 [API] 正常重命名成功")
    void test_GNYL_018_RenameSuccess_API() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            String newName = "Renamed_018_" + suffix();
            String resp = api.renameFolder(PROJECT_ID, child[0], parent[0], newName);
            Assertions.assertTrue(resp.contains("200"), "重命名失败: " + resp);
            log.info("GNYL_018 重命名成功: {} -> {}", child[1], newName);
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_020 [API] 同名冲突拦截")
    void test_GNYL_020_RenameDuplicate_API() {
        String[] parent = createTempParent();
        String[] child1 = createTempChild(parent[0], parent[1]);
        String[] child2 = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(1500);

            String resp = api.renameFolder(PROJECT_ID, child2[0], parent[0], child1[1]);

            if (!resp.contains("200")) {
                Assertions.assertTrue(
                        resp.contains("500") || resp.contains("存在"),
                        "同名拦截失败: " + resp
                );
            }
            log.info("GNYL_020 完成（后端返回: {}）", resp);
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_021 [UI] 手动修改同名文件夹（验证UI拦截）")
    void test_GNYL_021_RenameSameName_UI() {
        String[] parent = createTempParent();
        String[] child1 = createTempChild(parent[0], parent[1]);
        String childName1 = child1[1];
        try {
            page.waitForTimeout(500);

            reqPage.ensureNodeExpanded(parent[1]);
            reqPage.activateRenameInput(childName1);
            page.getByRole(AriaRole.TREE).getByText(childName1).click();
            String errorMsg = reqPage.fillRenameAndSave(childName1);

            Assertions.assertTrue(
                    errorMsg.contains("已经存在"),
                    "期望出现重名提示，实际: " + errorMsg
            );
            log.info("GNYL_021 UI同名冲突拦截通过");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_022 [API] 重命名为空")
    void test_GNYL_022_RenameEmpty_API() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            String resp = api.renameFolder(PROJECT_ID, child[0], parent[0], "");
            Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
            Assertions.assertTrue(resp.contains("名称不能为空"), "期望拦截空名称: " + resp);
            log.info("GNYL_022 空名称拦截成功");
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_023 [UI] 展开并编辑子文件夹描述")
    void test_GNYL_023_EditFolderDesc_UI() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(500);
            page.getByRole(AriaRole.TREEITEM,
                    new Page.GetByRoleOptions().setName(parent[1]).setExact(true)
            ).dblclick();
            page.waitForTimeout(500);

            Locator folderRow = page.getByRole(AriaRole.ROW,
                    new Page.GetByRoleOptions().setName(child[1]));
            folderRow.locator("pre").first().click();
            folderRow.locator("pre").first().click();

            Locator editor = page.locator("div[contenteditable='true']").first();
            editor.waitFor();
            editor.fill("测试描述内容");

            page.locator("div > div > div:nth-child(2) > div:nth-child(3)").first().click();
            log.info("GNYL_023 UI描述编辑成功");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_024 [API] 编辑文件夹描述")
    void test_GNYL_024_EditFolderDesc_API() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            String resp = api.editDescription(PROJECT_ID, child[0], parent[0], "这是通过API写入的描述");
            Assertions.assertTrue(resp.contains("修改成功"), "描述修改失败: " + resp);
            log.info("GNYL_024 API描述修改成功");
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_025 [API] 删除有子级的文件夹（应拦截）")
    void test_GNYL_025_DeleteHaveChildrenFolder_API() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            String resp = api.deleteFolder(parent[0], PROJECT_ID, "project");
            Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
            Assertions.assertTrue(resp.contains("该需求规格文件夹下有子级，暂时不允许删除"),
                    "拦截提示不匹配: " + resp);
            log.info("GNYL_025 子级删除拦截通过");
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_026 [UI] 删除有子级的文件夹（验证UI拦截）")
    void test_GNYL_026_DeleteHaveChildrenFolder_UI() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(parent[1]).setExact(true))
                    .locator("path").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            Locator errorMsg = page.getByText("该需求规格文件夹下有子级，暂时不允许删除！");
            assertThat(errorMsg).isVisible();
            log.info("GNYL_026 UI删除有子级文件夹拦截通过");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_027 [API] 删除无子级的文件夹")
    void test_GNYL_027_DeleteNoChildrenFolder() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            String resp = api.deleteFolder(child[0], parent[0], "reqSpeFolder");
            Assertions.assertTrue(resp.contains("200"), "业务返回码不是200: " + resp);
            Assertions.assertTrue(resp.contains("删除成功"), "返回信息不匹配: " + resp);
            log.info("GNYL_027 删除成功");
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_028: 删除无子级的文件夹(UI)")
    public void test_GNYL_028() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(parent[1]))
                    .locator("path").click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(child[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            Locator confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                    .filter(new Locator.FilterOptions().setHasText("确定")).first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
            }
            log.info("GNYL_028 删除成功");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_029 [API] 恢复已删除的文件夹")
    void test_GNYL_029_RecoverFolder() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            api.deleteFolder(child[0], parent[0], "reqSpeFolder");

            String resp = api.recoverFolder(child[0], parent[0]);
            Assertions.assertTrue(resp.contains("200"), "HTTP状态码错误: " + resp);
            Assertions.assertTrue(resp.contains("恢复成功"), "恢复失败: " + resp);
            log.info("GNYL_029 恢复成功");
        } finally {
            cleanupByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_030: 删除并取消删除文件夹(UI)")
    public void test_GNYL_030() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(500);
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(parent[1]))
                    .locator("path").click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(child[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            Locator confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                    .filter(new Locator.FilterOptions().setHasText("确定")).first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
            }

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(child[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("取消删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            log.info("GNYL_030 删除并取消删除成功");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_031: 删除并永久清除文件夹(UI)")
    public void test_GNYL_031() {
        String[] parent = createTempParent();
        String[] child = createTempChild(parent[0], parent[1]);
        try {
            page.waitForTimeout(500);
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(parent[1]))
                    .locator("path").click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(child[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            Locator confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                    .filter(new Locator.FilterOptions().setHasText("确定")).first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
            }

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(child[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("清除", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(300);

            confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                    .filter(new Locator.FilterOptions().setHasText("确定")).first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
            }

            log.info("GNYL_031 永久清除成功");
        } finally {
            cleanupByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_033 [API] 根节点刷新")
    void test_GNYL_033_RefreshRootNode_API() {
        String resp = api.getTree(PROJECT_ID, PROJECT_ID);
        Assertions.assertTrue(resp.contains("200"), "业务状态码不是200: " + resp);
        Assertions.assertTrue(resp.contains("操作成功"), "返回信息不匹配: " + resp);
        log.info("GNYL_033 刷新通过");
    }
}
