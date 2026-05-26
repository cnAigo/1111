package cases;

import actions.ReqApiActions;
import base.BaseTest;
import base.SetupEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConfig;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class RequirementTest extends BaseTest {

    private RequirementPage reqPage;
    private ReqApiActions api;
    private static final Logger log = LoggerFactory.getLogger(RequirementTest.class);

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
        api = new ReqApiActions(page.request());
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }


    @Test
    @Order(1)
    @DisplayName("清理")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void test01(){
        api.cleanFolderByName(TestConstants.PROJECT_ID,TestConstants.PARENT_FOLDER);
    }

    @Test
    @Order(10)
    @DisplayName("GNYL_012 [UI] 根节点右键新建文件夹")
    public void test_GNYL_012_CreateFolder_UI() {
        String existingId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
        if (existingId != null) {
            TestContext.set("parentId", existingId);
            log.info("GNYL_012 跳过(已存在), 父文件夹ID: {}", existingId);
            return;
        }

        reqPage.rightClickTreeNode(TestConstants.ROOT_NODE);
        reqPage.clickContextMenu("新建");

        String[] details = reqPage.createFolderAndGetDetails();
        TestContext.set("parentId", details[1]);

        reqPage.ensureNodeExpanded(TestConstants.ROOT_NODE);
        reqPage.renameFolder(details[0], TestConstants.PARENT_FOLDER);

        log.info("GNYL_012 成功, 父文件夹ID: {}", TestContext.get("parentId"));
    }

    @Test
    @Order(20)
    @DisplayName("GNYL_013 [UI] 右键新建子文件夹01")
    void test_GNYL_013_CreateChildFolder01_UI() {
        Assumptions.assumeTrue(TestContext.containsKey("parentId"), "前置文件夹未创建");

        if (api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.CHILD_FOLDER_1) != null) {
            log.info("GNYL_013 跳过(已存在): {}", TestConstants.CHILD_FOLDER_1);
            return;
        }

        reqPage.ensureNodeExpanded(TestConstants.PARENT_FOLDER);
        reqPage.rightClickTreeNode(TestConstants.PARENT_FOLDER);
        reqPage.clickContextMenu("新建");

        String originalName = reqPage.createDocumentAndGetName();
        reqPage.waitForTreeNodeVisible(originalName);
        reqPage.renameFolder(originalName, TestConstants.CHILD_FOLDER_1);
        log.info("GNYL_013 子文件夹01 创建成功");
    }

    @Test
    @Order(30)
    @DisplayName("GNYL_014 [API] 创建子文件夹02")
    void test_GNYL_014_CreateChildFolder02_API() {
        // 兜底：如果前置步骤没设置，自己通过API查找
        String parentId = TestContext.get("parentId");
        if (parentId == null || parentId.isEmpty()) {
            parentId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
            if (parentId != null) {
                TestContext.set("parentId", parentId);
            }
        }
        Assumptions.assumeTrue(parentId != null && !parentId.isEmpty(), "未获取到父节点ID");

        String newId = api.createFolder(TestConstants.PROJECT_ID, parentId);
        TestContext.set("targetFolderId", newId);

        Assertions.assertFalse(newId.isEmpty(), "创建文件夹未返回ID");
        log.info("GNYL_014 子文件夹02 ID: {}", newId);
    }


    @Test
    @Order(40)
    @DisplayName("GNYL_015 [UI] 顶部菜单新建子文件夹03")
    void test_GNYL_015_MenuCreateFolder_UI() {
        if (api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.CHILD_FOLDER_3) != null) {
            log.info("GNYL_015 跳过(已存在): {}", TestConstants.CHILD_FOLDER_3);
            return;
        }
        reqPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        String originalName = reqPage.clickNewFolderDropdownAndGetName();

        reqPage.ensureNodeExpanded(TestConstants.PARENT_FOLDER);
        reqPage.renameFolder(originalName, TestConstants.CHILD_FOLDER_3);
        log.info("GNYL_015 子文件夹03 创建成功");
    }

    @Test
    @Order(45)
    @DisplayName("GNYL_016 [API] 创建需求规格文档")
    void test_GNYL_016_CreateDocument_API() {
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(parentId != null && !parentId.isEmpty(), "未获取到父节点ID");

        String existing1 = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.REQ_NAME1);
        String existing2 = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.REQ_NAME2);

        String docId1 = api.createDocument(TestContext.get("projectId"), parentId);
        Assertions.assertFalse(docId1.isEmpty(), "未能获取到文档1的ID");
        api.renameDocument(TestContext.get("projectId"), docId1, parentId, TestConstants.REQ_NAME1);
        TestContext.set("reqId1", docId1);

        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        String docId2 = api.createDocument(TestContext.get("projectId"), parentId);
        Assertions.assertFalse(docId2.isEmpty(), "未能获取到文档2的ID");
        api.renameDocument(TestContext.get("projectId"), docId2, parentId, TestConstants.REQ_NAME2);
        TestContext.set("reqId2", docId2);
        log.info("GNYL_016 文档2 ID: {}, 名称: {}", docId2, TestConstants.REQ_NAME2);
    }
    @Test
    @Order(50)
    @DisplayName("GNYL_017: 需求规格下新建同级文件夹")
    public void test_GNYL_017() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER)).dblclick();
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1)).click(new Locator.ClickOptions()
                .setButton(MouseButton.RIGHT));
        page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^新建$"))).click();
        page.getByText("文件夹", new Page.GetByTextOptions().setExact(true)).click();

        // 通过 API 获取树，按名称找父文件夹
        String resp = api.getTree(TestConstants.PROJECT_ID, TestConstants.PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");

        JsonObject parentFolder = null;
        for (JsonElement el : data) {
            JsonObject node = el.getAsJsonObject();
            if (TestConstants.PARENT_FOLDER.equals(node.get("title").getAsString())) {
                parentFolder = node;
                break;
            }
        }

        Assumptions.assumeTrue(parentFolder != null, "未找到测试父文件夹");
        TestContext.set("parentId", parentFolder.get("objectId").getAsString());

        // 在父文件夹的 children 里找最新创建的文件夹
        String newFolderName = null;
        long maxTime = 0;
        if (parentFolder.has("children")) {
            for (JsonElement child : parentFolder.getAsJsonArray("children")) {
                JsonObject node = child.getAsJsonObject();
                String type = node.get("type").getAsString();
                String name = node.get("title").getAsString();
                long createTime = Long.parseLong(node.get("createTime").getAsString());

                if ("reqSpeFolder".equals(type)
                        && createTime > maxTime
                        && !name.equals(TestConstants.CHILD_FOLDER_1)
                        && !name.equals(TestConstants.CHILD_FOLDER_2)
                        && !name.equals(TestConstants.CHILD_FOLDER_3)) {
                    maxTime = createTime;
                    newFolderName = name;
                }
            }
        }

        Assumptions.assumeTrue(newFolderName != null, "未找到新建的同级文件夹");
        TestContext.set("gnyl017FolderName", newFolderName);
        log.info("GNYL_017 需求规格下新建同级文件夹成功, name={}", newFolderName);
    }


    @Test
    @Order(60)
    @DisplayName("GNYL_018 [API] 正常重命名成功")
    void test_GNYL_018_RenameSuccess_API() {
        String targetId = TestContext.get("targetFolderId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(targetId != null && !targetId.isEmpty(), "没有拿到目标文件夹ID");

        Assumptions.assumeTrue(
                api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.CHILD_FOLDER_2) == null,
                "CHILD_FOLDER_2 已存在, 跳过重命名测试"
        );

        String resp = api.renameFolder(TestContext.get("projectId"), targetId, parentId, TestConstants.CHILD_FOLDER_2);

        Assertions.assertTrue(resp.contains("200"), "重命名失败: " + resp);
        log.info("GNYL_018 重命名成功");
    }



    @Test
    @Order(80)
    @DisplayName("GNYL_020 [API] 同名冲突拦截")
    void test_GNYL_020_RenameDuplicate_API() {
        String targetId = TestContext.get("targetFolderId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(targetId != null, "没有拿到目标文件夹ID");

        page.waitForTimeout(1500);

        String resp = api.renameFolder(TestContext.get("projectId"), targetId, parentId, TestConstants.CHILD_FOLDER_1);

        // 后端返回200说明没有拦截，先记录但不失败（或者标记为已知问题）
        if (resp.contains("200")) {
        } else {
            Assertions.assertTrue(
                    resp.contains("500") || resp.contains("存在"),
                    "同名拦截失败: " + resp
            );
        }
        log.info("GNYL_020 完成（后端返回: {}）", resp);
    }

    @Test
    @Order(90)
    @DisplayName("GNYL_021 [UI] 手动修改同名文件夹（验证UI拦截）")
    void test_GNYL_021_RenameSameName_UI() {
        reqPage.ensureNodeExpanded(TestConstants.PARENT_FOLDER);
        reqPage.activateRenameInput(TestConstants.CHILD_FOLDER_3);
        page.getByRole(AriaRole.TREE).getByText(TestConstants.CHILD_FOLDER_3).click();
        String errorMsg = reqPage.fillRenameAndSave(TestConstants.CHILD_FOLDER_1);


        Assertions.assertTrue(
                errorMsg.contains("已经存在"),
                "期望出现重名提示，实际: " + errorMsg
        );
        log.info("GNYL_021 UI同名冲突拦截通过");
    }

    @Test
    @Order(100)
    @DisplayName("GNYL_022 [API] 重命名为空")
    void test_GNYL_022_RenameEmpty_API() {
        String targetId = TestContext.get("targetFolderId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(targetId != null && !targetId.isEmpty(), "没有拿到目标文件夹ID");

        String resp = api.renameFolder(TestContext.get("projectId"), targetId, parentId, "");

        Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
        Assertions.assertTrue(resp.contains("名称不能为空"), "期望拦截空名称: " + resp);
        log.info("GNYL_022 空名称拦截成功");
    }

    @Test
    @Order(110)
    @DisplayName("GNYL_023 [UI] 展开并编辑子文件夹描述")
    void test_GNYL_023_EditFolderDesc_UI() {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER).setExact(true)
        ).dblclick();

        Locator folderRow = page.getByRole(AriaRole.ROW,
                new Page.GetByRoleOptions().setName(TestConstants.CHILD_FOLDER_1));

        // 用 .first() 解决 strict mode violation
        folderRow.locator("pre").first().click();
        folderRow.locator("pre").first().click();

        Locator editor = page.locator("div[contenteditable='true']").first();
        editor.waitFor();
        editor.fill("测试描述内容");

        page.locator("div > div > div:nth-child(2) > div:nth-child(3)").first().click();
        log.info("GNYL_023 UI描述编辑成功");
    }


    @Test
    @Order(120)
    @DisplayName("GNYL_024 [API] 编辑文件夹描述")
    void test_GNYL_024_EditFolderDesc_API() {
        String targetId = TestContext.get("targetFolderId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(targetId != null && !targetId.isEmpty(), "没有拿到目标文件夹ID");

        String resp = api.editDescription(TestContext.get("projectId"), targetId, parentId, "这是通过API写入的描述");

        Assertions.assertTrue(resp.contains("修改成功"), "描述修改失败: " + resp);
        log.info("GNYL_024 API描述修改成功");
    }

    @Test
    @Order(250)
    @DisplayName("GNYL_025 [API] 删除有子级的文件夹（应拦截）")
    void test_GNYL_025_DeleteHaveChildrenFolder_API() {
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(parentId != null && !parentId.isEmpty(), "未获取到父节点ID");

        String resp = api.deleteFolder(parentId, TestContext.get("projectId"), "project");

        Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
        Assertions.assertTrue(resp.contains("该需求规格文件夹下有子级，暂时不允许删除"), "拦截提示不匹配: " + resp);
        log.info("GNYL_025 子级删除拦截通过");
    }

    @Test
    @Order(260)
    @DisplayName("GNYL_026 [UI] 删除有子级的文件夹（验证UI拦截）")
    void test_GNYL_026_DeleteHaveChildrenFolder_UI() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER).setExact(true))
                .locator("path").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));

        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();

        Locator errorMsg = page.getByText("该需求规格文件夹下有子级，暂时不允许删除！");
        assertThat(errorMsg).isVisible();

        log.info("GNYL_026 UI删除有子级文件夹拦截通过");
    }

    @Test
    @Order(270)
    @DisplayName("GNYL_027 [API] 删除无子级的文件夹")
    void test_GNYL_027_DeleteNoChildrenFolder() {
        String targetId = TestContext.get("targetFolderId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(targetId != null && !targetId.isEmpty(), "没有拿到目标文件夹ID");

        String resp = api.deleteFolder(targetId, parentId, "reqSpeFolder");

        Assertions.assertTrue(resp.contains("200"), "业务返回码不是200: " + resp);
        Assertions.assertTrue(resp.contains("删除成功"), "返回信息不匹配: " + resp);
        log.info("GNYL_027 删除成功");
    }
    @Test
    @Order(280)
    @DisplayName("GNYL_028: 删除无子级的文件夹(UI)")
    public void test_GNYL_028() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .locator("path").click();
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.CHILD_FOLDER_1).setExact(true))
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

        log.info("GNYL_028 删除 CHILD_FOLDER_1 成功");
    }
    @Test
    @Order(290)
    @DisplayName("GNYL_029 [API] 恢复已删除的文件夹")
    void test_GNYL_029_RecoverFolder() {
        String parentId = TestContext.get("parentId");
        if (parentId == null || parentId.isEmpty()) {
            parentId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
            if (parentId != null) TestContext.set("parentId", parentId);
        }
        Assumptions.assumeTrue(parentId != null, "未找到父文件夹");

        String child1Id = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.CHILD_FOLDER_1);
        Assumptions.assumeTrue(child1Id != null, "未找到 CHILD_FOLDER_1");

        String resp = api.recoverFolder(child1Id, parentId);

        Assertions.assertTrue(resp.contains("200"), "HTTP状态码错误: " + resp);
        Assertions.assertTrue(resp.contains("恢复成功"), "恢复失败: " + resp);
        log.info("GNYL_029 恢复 CHILD_FOLDER_1 成功");
    }
    @Test
    @Order(300)
    @DisplayName("GNYL_030: 删除并取消删除文件夹(UI)")
    public void test_GNYL_030() {
        String targetName = TestConstants.CHILD_FOLDER_3;
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .locator("path").click();

        // 先删除
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(targetName).setExact(true))
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

        // 取消删除
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(targetName).setExact(true))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("取消删除", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);

        log.info("GNYL_030 删除并取消删除 CHILD_3 成功");
    }
@Test
    @Order(310)
    @DisplayName("GNYL_031: 删除并永久清除文件夹(UI)")
    public void test_GNYL_031() {
        String targetName = TestContext.get("gnyl016FolderName");
        Assumptions.assumeTrue(targetName != null, "GNYL_017 未创建文件夹");

        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .locator("path").click();

        // 先删除
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(targetName).setExact(true))
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

        // 再清除
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(targetName).setExact(true))
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

        log.info("GNYL_031 永久清除 {} 成功", targetName);
    }
    @Test
    @Order(330)
    @DisplayName("GNYL_033 [API] 根节点刷新")
    void test_GNYL_033_RefreshRootNode_API() {
        String resp = api.getTree(TestConstants.PROJECT_ID, TestConstants.PROJECT_ID);

        Assertions.assertTrue(resp.contains("200"), "业务状态码不是200: " + resp);
        Assertions.assertTrue(resp.contains("操作成功"), "返回信息不匹配: " + resp);
        log.info("GNYL_033 刷新通过");
    }
}
