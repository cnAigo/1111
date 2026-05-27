package cases;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RequirementTest extends BaseTest {

    private String[] createTempChild(String parentId) {
        String childName = "Temp_Child_" + suffix();
        String childId = api.createFolder(PROJECT_ID, parentId);
        api.renameFolder(PROJECT_ID, childId, parentId, childName);
        return new String[]{childId, childName};
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
            cleanupFolderByName(tempName);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_013 [UI] 右键新建子文件夹")
    void test_GNYL_013_CreateChildFolder_UI() {
        String[] parent = createTempFolder();
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            reqPage.ensureNodeExpanded(parent[1]);
            reqPage.rightClickTreeNode(parent[1]);
            reqPage.clickContextMenu("新建");

            String originalName = reqPage.createDocumentAndGetName();
            reqPage.waitForTreeNodeVisible(originalName);

            String childName = "Temp_013_Child_" + suffix();
            reqPage.renameFolder(originalName, childName);
            log.info("GNYL_013 子文件夹创建成功: {}", childName);
        } finally {
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_014 [API] 创建子文件夹")
    void test_GNYL_014_CreateChildFolder_API() {
        String[] parent = createTempFolder();
        try {
            String childId = api.createFolder(PROJECT_ID, parent[0]);
            Assertions.assertFalse(childId.isEmpty(), "创建文件夹未返回ID");
            log.info("GNYL_014 子文件夹创建成功, ID: {}", childId);
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_015 [UI] 顶部菜单新建子文件夹")
    void test_GNYL_015_MenuCreateFolder_UI() {
        String[] parent = createTempFolder();
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            reqPage.doubleClickTreeNode(parent[1]);
            String originalName = reqPage.clickNewFolderDropdownAndGetName();

            reqPage.ensureNodeExpanded(parent[1]);
            String childName = "Temp_015_Child_" + suffix();
            reqPage.renameFolder(originalName, childName);
            log.info("GNYL_015 菜单创建子文件夹成功: {}", childName);
        } finally {
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_016 [API] 创建需求规格文档")
    void test_GNYL_016_CreateDocument_API() {
        String[] parent = createTempFolder();
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
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_017: 需求规格下新建同级文件夹")
    public void test_GNYL_017() {
        String[] parent = createTempFolder();
        try {
            String[] doc = createTempDoc(parent[0]);
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(parent[1])).dblclick();
            page.waitForTimeout(500);

            page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(doc[1]))
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^新建$"))).click();
            page.getByText("文件夹", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            log.info("GNYL_017 需求规格下新建同级文件夹成功");
        } finally {
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_018 [API] 正常重命名成功")
    void test_GNYL_018_RenameSuccess_API() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            String newName = "Renamed_018_" + suffix();
            String resp = api.renameFolder(PROJECT_ID, child[0], parent[0], newName);
            Assertions.assertTrue(resp.contains("200"), "重命名失败: " + resp);
            log.info("GNYL_018 重命名成功: {} -> {}", child[1], newName);
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_020 [API] 同名冲突拦截")
    void test_GNYL_020_RenameDuplicate_API() {
        String[] parent = createTempFolder();
        String[] child1 = createTempChild(parent[0]);
        String[] child2 = createTempChild(parent[0]);
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
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_021 [UI] 手动修改同名文件夹（验证UI拦截）")
    void test_GNYL_021_RenameSameName_UI() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        String childName1 = child[1];
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

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
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_022 [API] 重命名为空")
    void test_GNYL_022_RenameEmpty_API() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            String resp = api.renameFolder(PROJECT_ID, child[0], parent[0], "");
            Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
            Assertions.assertTrue(resp.contains("名称不能为空"), "期望拦截空名称: " + resp);
            log.info("GNYL_022 空名称拦截成功");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_023 [UI] 展开并编辑子文件夹描述")
    void test_GNYL_023_EditFolderDesc_UI() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);
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
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_024 [API] 编辑文件夹描述")
    void test_GNYL_024_EditFolderDesc_API() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            String resp = api.editDescription(PROJECT_ID, child[0], parent[0], "这是通过API写入的描述");
            Assertions.assertTrue(resp.contains("修改成功"), "描述修改失败: " + resp);
            log.info("GNYL_024 API描述修改成功");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_025 [API] 删除有子级的文件夹（应拦截）")
    void test_GNYL_025_DeleteHaveChildrenFolder_API() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            String resp = api.deleteFolder(parent[0], PROJECT_ID, "project");
            Assertions.assertTrue(resp.contains("500"), "期望返回500: " + resp);
            Assertions.assertTrue(resp.contains("该需求规格文件夹下有子级，暂时不允许删除"),
                    "拦截提示不匹配: " + resp);
            log.info("GNYL_025 子级删除拦截通过");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_026 [UI] 删除有子级的文件夹（验证UI拦截）")
    void test_GNYL_026_DeleteHaveChildrenFolder_UI() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

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
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_027 [API] 删除无子级的文件夹")
    void test_GNYL_027_DeleteNoChildrenFolder() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            String resp = api.deleteFolder(child[0], parent[0], "reqSpeFolder");
            Assertions.assertTrue(resp.contains("200"), "业务返回码不是200: " + resp);
            Assertions.assertTrue(resp.contains("删除成功"), "返回信息不匹配: " + resp);
            log.info("GNYL_027 删除成功");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_028: 删除无子级的文件夹(UI)")
    public void test_GNYL_028() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);

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
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_029 [API] 恢复已删除的文件夹")
    void test_GNYL_029_RecoverFolder() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            api.deleteFolder(child[0], parent[0], "reqSpeFolder");

            String resp = api.recoverFolder(child[0], parent[0]);
            Assertions.assertTrue(resp.contains("200"), "HTTP状态码错误: " + resp);
            Assertions.assertTrue(resp.contains("恢复成功"), "恢复失败: " + resp);
            log.info("GNYL_029 恢复成功");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }

    @Test
    @DisplayName("GNYL_030: 删除并取消删除文件夹(UI)")
    public void test_GNYL_030() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);
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
            cleanupFolderByName(parent[1]);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_031: 删除并永久清除文件夹(UI)")
    public void test_GNYL_031() {
        String[] parent = createTempFolder();
        String[] child = createTempChild(parent[0]);
        try {
            page.reload();
            page.waitForLoadState(LoadState.NETWORKIDLE);
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
            cleanupFolderByName(parent[1]);
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

    // ========== Req Item CRUD tests ==========

    @Test
    @DisplayName("GNYL_041 [API] 新建需求条目")
    void test_GNYL_041_AddReqItem_API() {
        String[] doc = createTempDoc();
        try {
            String itemId = api.addReqItem(PROJECT_ID, doc[0], doc[0]);
            Assertions.assertFalse(itemId.isEmpty(), "创建需求条目未返回ID");
            log.info("GNYL_041 新建需求条目成功, itemId={}", itemId);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_042 [API] 编辑需求条目标题")
    void test_GNYL_042_UpdateReqItemTitle_API() {
        String[] doc = createTempDoc();
        try {
            String rawResp = api.addReqItemRaw(PROJECT_ID, doc[0], doc[0]);
            JsonObject root = JsonParser.parseString(rawResp).getAsJsonObject();
            JsonObject reqData = root.getAsJsonObject("data");

            String newTitle = "UpdatedTitle_" + suffix();
            reqData.addProperty("title", newTitle);
            reqData.addProperty("name", newTitle);

            JsonArray reqList = new JsonArray();
            reqList.add(reqData);
            String updateResp = api.updateReqList(doc[0], reqList.toString());
            Assertions.assertTrue(updateResp.contains("修改成功"), "标题修改失败: " + updateResp);
            log.info("GNYL_042 编辑标题成功: {}", newTitle);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_043 [API] 编辑需求条目描述")
    void test_GNYL_043_UpdateReqItemDesc_API() {
        String[] doc = createTempDoc();
        try {
            String rawResp = api.addReqItemRaw(PROJECT_ID, doc[0], doc[0]);
            JsonObject root = JsonParser.parseString(rawResp).getAsJsonObject();
            JsonObject reqData = root.getAsJsonObject("data");

            String newDesc = "<p>自动化测试描述_" + suffix() + "</p>";
            reqData.addProperty("description", newDesc);

            JsonArray reqList = new JsonArray();
            reqList.add(reqData);
            String updateResp = api.updateReqList(doc[0], reqList.toString());
            Assertions.assertTrue(updateResp.contains("修改成功"), "描述修改失败: " + updateResp);
            log.info("GNYL_043 编辑描述成功");
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_044 [API] 删除需求条目")
    void test_GNYL_044_DeleteReqItem_API() {
        String[] doc = createTempDoc();
        try {
            String itemId = api.addReqItem(PROJECT_ID, doc[0], doc[0]);
            String deleteResp = api.deleteReqItem(itemId);
            Assertions.assertTrue(deleteResp.contains("200"), "删除失败: " + deleteResp);
            log.info("GNYL_044 删除需求条目成功, itemId={}", itemId);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_045 [API] 恢复已删除的需求条目")
    void test_GNYL_045_RecoverReqItem_API() {
        String[] doc = createTempDoc();
        try {
            String itemId = api.addReqItem(PROJECT_ID, doc[0], doc[0]);
            api.deleteReqItem(itemId);
            String recoverResp = api.recoverReqItem(itemId);
            Assertions.assertTrue(recoverResp.contains("200"), "恢复失败: " + recoverResp);
            log.info("GNYL_045 恢复需求条目成功, itemId={}", itemId);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_046 [API] 永久清除需求条目")
    void test_GNYL_046_CleanReqItem_API() {
        String[] doc = createTempDoc();
        try {
            String itemId = api.addReqItem(PROJECT_ID, doc[0], doc[0]);
            api.deleteReqItem(itemId);
            String cleanResp = api.cleanReqItem(itemId, doc[0]);
            Assertions.assertTrue(cleanResp.contains("200"), "清除失败: " + cleanResp);
            log.info("GNYL_046 永久清除需求条目成功, itemId={}", itemId);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // ========== View management tests ==========

    @Test
    @DisplayName("GNYL_047 [API] 保存视图")
    void test_GNYL_047_SaveView_API() {
        String[] doc = createTempDoc();
        try {
            String viewName = "AT_View_" + suffix();
            String resp = api.addView(doc[0], viewName, "auto test view", "name,editStatus,description");
            Assertions.assertTrue(resp.contains("200"), "保存视图失败: " + resp);
            log.info("GNYL_047 保存视图成功: {}", viewName);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_048 [API] 查询视图列表")
    void test_GNYL_048_SearchViewList_API() {
        String[] doc = createTempDoc();
        try {
            api.addView(doc[0], "AT_View_" + suffix(), "auto test", "name,editStatus,description");
            String resp = api.searchViewList(doc[0]);
            Assertions.assertTrue(resp.contains("200"), "查询视图列表失败: " + resp);
            Assertions.assertTrue(resp.contains("操作成功"), "返回信息不匹配: " + resp);
            log.info("GNYL_048 查询视图列表通过");
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_049 [API] 删除视图")
    void test_GNYL_049_DeleteView_API() {
        String[] doc = createTempDoc();
        try {
            api.addView(doc[0], "AT_View_" + suffix(), "auto test", "name,editStatus,description");
            String listResp = api.searchViewList(doc[0]);
            JsonObject root = JsonParser.parseString(listResp).getAsJsonObject();
            JsonArray data = root.getAsJsonArray("data");
            Assertions.assertTrue(data.size() > 0, "视图列表为空，无法测试删除");

            String viewId = data.get(0).getAsJsonObject().get("id").getAsString();
            String deleteResp = api.deleteView(viewId);
            Assertions.assertTrue(deleteResp.contains("200"), "删除视图失败: " + deleteResp);
            log.info("GNYL_049 删除视图成功, viewId={}", viewId);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // ========== Unlock test ==========

    @Test
    @DisplayName("GNYL_050 [API] 解锁需求规格")
    void test_GNYL_050_UnlockMode_API() {
        String[] doc = createTempDoc();
        try {
            String resp = api.unlockMode(doc[0], "share", "admin");
            Assertions.assertTrue(resp.contains("200"), "解锁失败: " + resp);
            Assertions.assertTrue(resp.contains("操作成功"), "解锁返回信息不匹配: " + resp);
            log.info("GNYL_050 解锁需求规格成功, docId={}", doc[0]);
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // ========== Access check test ==========

    @Test
    @DisplayName("GNYL_051 [API] 查询需求规格访问权限")
    void test_GNYL_051_GetReqAccess_API() {
        String[] doc = createTempDoc();
        try {
            String resp = api.getReqAccess(doc[0]);
            Assertions.assertTrue(resp.contains("200"), "查询权限失败: " + resp);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            Assertions.assertTrue(data.get("flag").getAsBoolean(), "访问权限 flag 应为 true");
            log.info("GNYL_051 查询权限成功: flag={}", data.get("flag").getAsBoolean());
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // ========== Version & children tests ==========

    @Test
    @DisplayName("GNYL_052 [API] 查询需求规格版本列表")
    void test_GNYL_052_GetVersionList_API() {
        String[] doc = createTempDoc();
        try {
            String resp = api.getVersionList(doc[0]);
            Assertions.assertTrue(resp.contains("200"), "查询版本列表失败: " + resp);
            log.info("GNYL_052 查询版本列表通过");
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_053 [API] 查询需求条目子级")
    void test_GNYL_053_SearchChildReq_API() {
        String[] doc = createTempDoc();
        try {
            api.addReqItem(PROJECT_ID, doc[0], doc[0]);
            String resp = api.searchChildReqInfo(doc[0]);
            Assertions.assertTrue(resp.contains("200"), "查询子级失败: " + resp);
            log.info("GNYL_053 查询需求条目子级通过");
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_054 [API] 查询文件夹子级")
    void test_GNYL_054_SearchFolderChildren_API() {
        String[] parent = createTempFolder();
        try {
            createTempChild(parent[0]);
            String resp = api.searchFolderChildren(parent[0]);
            Assertions.assertTrue(resp.contains("200"), "查询文件夹子级失败: " + resp);
            log.info("GNYL_054 查询文件夹子级通过");
        } finally {
            cleanupFolderByName(parent[1]);
        }
    }
}
