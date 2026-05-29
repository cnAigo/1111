package cases.api;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FolderApiTest extends ApiTestHelper {

    private static final String PT_PROJECT = "project";

    // ==================== 新建文件夹 ====================

    @Test
    @DisplayName("GNYL_012: 根节点下新建文件夹")
    void test_GNYL_012_createFolderUnderRoot() {
        String folderId = null;
        String folderName = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(folderName),
                    "树结构中应包含新建的文件夹: " + folderName);
            log.info("GNYL_012 通过: 新建文件夹 [{}] id={}", folderName, folderId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_012-N: 新建文件夹-名称为空(负向)")
    void test_GNYL_012N_createFolderEmptyName() {
        String folderId = null;
        try {
            folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "空名称应返回错误码, 实际code=" + code + ", resp: " + resp);
            log.info("GNYL_012-N 通过: 空名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_013: 文件夹下新建子文件夹(正向)")
    void test_GNYL_013_createSubFolder() {
        String parentId = null;
        String childId = null;
        try {
            parentId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String parentName = "AT_Parent_" + suffix();
            api.renameFolder(PROJECT_ID, parentId, PROJECT_ID, parentName);

            childId = api.createFolder(PROJECT_ID, parentId);
            String childName = "AT_Child_" + suffix();
            api.renameFolder(PROJECT_ID, childId, parentId, childName);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(parentName), "树中应有父文件夹");
            Assertions.assertTrue(treeResp.contains(childName), "树中应有子文件夹");
            log.info("GNYL_013 通过: 子文件夹 [{}] 创建在 [{}] 下", childName, parentName);
        } finally {
            if (childId != null) hardCleanFolder(childId);
            if (parentId != null) hardCleanFolder(parentId);
        }
    }

    @Test
    @DisplayName("GNYL_016: 文件夹下新建同级文件夹(正向)")
    void test_GNYL_016_createSiblingFolder() {
        String parentId = null;
        String child1Id = null;
        String child2Id = null;
        try {
            parentId = api.createFolder(PROJECT_ID, PROJECT_ID);
            api.renameFolder(PROJECT_ID, parentId, PROJECT_ID, "AT_Parent_" + suffix());

            child1Id = api.createFolder(PROJECT_ID, parentId);
            api.renameFolder(PROJECT_ID, child1Id, parentId, "AT_Child1_" + suffix());

            child2Id = api.createFolder(PROJECT_ID, parentId);
            api.renameFolder(PROJECT_ID, child2Id, parentId, "AT_Child2_" + suffix());

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains("AT_Child1_"), "树中应有第一个子文件夹");
            Assertions.assertTrue(treeResp.contains("AT_Child2_"), "树中应有同级子文件夹");
            log.info("GNYL_016 通过: 同级文件夹创建成功");
        } finally {
            if (child1Id != null) hardCleanFolder(child1Id);
            if (child2Id != null) hardCleanFolder(child2Id);
            if (parentId != null) hardCleanFolder(parentId);
        }
    }

    // ==================== 修改文件夹 ====================

    @Test
    @DisplayName("GNYL_018: 修改为不存在的文件夹名称(正向)")
    void test_GNYL_018_renameFolderValid() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String oldName = f[1];

            String newName = "AT_Renamed_" + suffix();
            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, newName);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "重命名应成功, resp: " + resp);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(newName),
                    "树结构中应包含新名称: " + newName);
            Assertions.assertFalse(treeResp.contains(oldName),
                    "树结构中不应包含旧名称: " + oldName);
            log.info("GNYL_018 通过: 重命名 {} -> {}", oldName, newName);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_020: 修改为重复的文件夹名称(负向)")
    void test_GNYL_020_renameFolderDuplicate() {
        String folderId1 = null;
        String folderId2 = null;
        try {
            folderId1 = api.createFolder(PROJECT_ID, PROJECT_ID);
            String name1 = "AT_Dup1_" + suffix();
            api.renameFolder(PROJECT_ID, folderId1, PROJECT_ID, name1);

            folderId2 = api.createFolder(PROJECT_ID, PROJECT_ID);
            String name2 = "AT_Dup2_" + suffix();
            api.renameFolder(PROJECT_ID, folderId2, PROJECT_ID, name2);

            String resp = api.renameFolder(PROJECT_ID, folderId2, PROJECT_ID, name1);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "重命名为重复名称应失败, 实际code=" + code + ", resp: " + resp);
            log.info("GNYL_020 通过: 重复名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId1 != null) hardCleanFolder(folderId1);
            if (folderId2 != null) hardCleanFolder(folderId2);
        }
    }

    @Test
    @DisplayName("GNYL_022: 文件夹名称为空(负向)")
    void test_GNYL_022_renameFolderEmptyName() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String oldName = f[1];

            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, oldName);
                Assertions.fail("后端未拦截空名称, code=200, 疑似缺陷");
            }
            log.info("GNYL_022 通过: 空名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_022-L: 文件夹名称超长(负向)")
    void test_GNYL_022L_renameFolderTooLong() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String oldName = f[1];

            String longName = "A".repeat(200);
            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, longName);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, oldName);
                log.warn("超长名称(200字符)未被拦截, code=200, 疑似缺陷");
            }
            log.info("GNYL_022-L: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("重命名文件夹-特殊字符(负向)")
    void test_renameFolderSpecialChars() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String oldName = f[1];

            String specialName = "测试<script>alert(1)</script>";
            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, specialName);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, oldName);
                log.warn("特殊字符名称未被拦截, code=200, 可能存在XSS风险");
            }
            log.info("重命名-特殊字符: code={}", code);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("重命名不存在的文件夹(负向)")
    void test_renameNonExistingFolder() {
        String resp = api.renameFolder(PROJECT_ID, "nonexistent_id_99999", PROJECT_ID, "test");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code, "不存在的文件夹重命名应失败");
        log.info("重命名不存在的文件夹 通过: code={}", code);
    }

    // ==================== 删除文件夹 ====================

    @Test
    @DisplayName("GNYL_027: 删除无子元素的文件夹(正向)")
    void test_GNYL_027_deleteEmptyFolder() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String folderName = f[1];

            String resp = api.deleteFolder(folderId, PROJECT_ID, PT_PROJECT);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除应成功, resp: " + resp);
            log.info("GNYL_027 通过: 删除空文件夹 [{}] 成功", folderName);
            folderId = null;
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_029: 取消删除(恢复)文件夹(正向)")
    void test_GNYL_029_recoverFolder() {
        String folderId = null;
        String folderName = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];

            api.deleteFolder(folderId, PROJECT_ID, PT_PROJECT);

            String resp = api.recoverFolder(folderId, PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "恢复应成功, resp: " + resp);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(folderName),
                    "恢复后树结构中应包含该文件夹: " + folderName);
            log.info("GNYL_029 通过: 恢复文件夹 [{}] 成功", folderName);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_031: 彻底删除文件夹(正向)")
    void test_GNYL_031_forceCleanFolder() {
        String folderId = null;
        String folderName = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];

            api.deleteFolder(folderId, PROJECT_ID, PT_PROJECT);
            api.forceCleanFolder(folderId);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertFalse(treeResp.contains(folderName),
                    "彻底删除后树结构中不应包含该文件夹: " + folderName);
            log.info("GNYL_031 通过: 彻底删除文件夹 [{}] 成功", folderName);
            folderId = null;
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 递归删除/恢复/彻底删除 ====================

    @Test
    @DisplayName("GNYL_023/024: 删除含子元素文件夹(正向)")
    void test_GNYL_023_deleteFolderWithChildren() {
        String parentId = null;
        String childId = null;
        String docId = null;
        try {
            parentId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String parentName = "AT_ParentDel_" + suffix();
            api.renameFolder(PROJECT_ID, parentId, PROJECT_ID, parentName);

            childId = api.createFolder(PROJECT_ID, parentId);
            api.renameFolder(PROJECT_ID, childId, parentId, "AT_Child_" + suffix());

            docId = api.createDocument(PROJECT_ID, parentId);
            api.renameDocument(PROJECT_ID, docId, parentId, "AT_Doc_" + suffix());

            api.deleteDocument(docId, parentId);
            api.forceCleanDocument(docId, parentId);
            docId = null;

            api.deleteFolder(childId, parentId, "reqSpeFolder");
            api.forceCleanFolder(childId);
            childId = null;

            String resp = api.deleteFolder(parentId, PROJECT_ID, PT_PROJECT);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除含子元素的文件夹应成功, resp: " + resp);

            api.forceCleanFolder(parentId);
            parentId = null;
            log.info("GNYL_023/024 通过: 删除含子元素文件夹成功");
        } finally {
            if (childId != null) hardCleanFolder(childId);
            if (docId != null) cleanupDoc(docId, parentId != null ? parentId : PROJECT_ID);
            if (parentId != null) hardCleanFolder(parentId);
        }
    }

    @Test
    @DisplayName("GNYL_028/030/032: 多级删除-恢复-彻底删除(正向)")
    void test_multiLevelDeleteRecoverClean() {
        String grandParentId = null;
        String parentId = null;
        String childId = null;
        String grandParentName = null;
        try {
            grandParentId = api.createFolder(PROJECT_ID, PROJECT_ID);
            grandParentName = "AT_GrandParent_" + suffix();
            api.renameFolder(PROJECT_ID, grandParentId, PROJECT_ID, grandParentName);

            parentId = api.createFolder(PROJECT_ID, grandParentId);
            String parentName = "AT_Parent_" + suffix();
            api.renameFolder(PROJECT_ID, parentId, grandParentId, parentName);

            childId = api.createFolder(PROJECT_ID, parentId);
            String childName = "AT_Child_" + suffix();
            api.renameFolder(PROJECT_ID, childId, parentId, childName);

            // 逐级删除
            api.deleteFolder(childId, parentId, "reqSpeFolder");
            api.deleteFolder(parentId, grandParentId, "reqSpeFolder");
            api.deleteFolder(grandParentId, PROJECT_ID, PT_PROJECT);
            log.info("多级删除完成");

            // 逐级恢复
            api.recoverFolder(grandParentId, PROJECT_ID);
            api.recoverFolder(parentId, grandParentId);
            api.recoverFolder(childId, parentId);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(grandParentName),
                    "恢复后树中应有顶层文件夹");
            log.info("多级恢复完成");

            // 逐级彻底删除
            api.deleteFolder(childId, parentId, "reqSpeFolder");
            api.forceCleanFolder(childId);
            api.deleteFolder(parentId, grandParentId, "reqSpeFolder");
            api.forceCleanFolder(parentId);
            api.deleteFolder(grandParentId, PROJECT_ID, PT_PROJECT);
            api.forceCleanFolder(grandParentId);

            treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertFalse(treeResp.contains(grandParentName),
                    "彻底删除后树中不应有顶层文件夹");
            log.info("多级彻底删除完成");

            childId = null;
            parentId = null;
            grandParentId = null;
        } finally {
            if (childId != null) hardCleanFolder(childId);
            if (parentId != null) hardCleanFolder(parentId);
            if (grandParentId != null) hardCleanFolder(grandParentId);
        }
    }

    // ==================== 删除负向 ====================

    @Test
    @DisplayName("删除不存在的文件夹(负向)")
    void test_deleteNonExistingFolder() {
        String resp = api.deleteFolder("nonexistent_id_99999", PROJECT_ID, PT_PROJECT);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code, "删除不存在的文件夹应失败");
        log.info("删除不存在的文件夹 通过: code={}", code);
    }

    @Test
    @DisplayName("恢复不存在的文件夹(负向)")
    void test_recoverNonExistingFolder() {
        String resp = api.recoverFolder("nonexistent_id_99999", PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code, "恢复不存在的文件夹应失败");
        log.info("恢复不存在的文件夹 通过: code={}", code);
    }

    @Test
    @DisplayName("空parentId新建文件夹(负向)")
    void test_createFolderEmptyParentId() {
        String resp = api.renameFolder(PROJECT_ID, "", PROJECT_ID, "test");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code, "空parentId应被拦截");
        log.info("空parentId新建 通过: code={}", code);
    }

    // ==================== 查询文件夹子元素 ====================

    @Test
    @DisplayName("查询文件夹直接子元素(正向)")
    void test_searchFolderChildren() {
        String folderId = null;
        String childId = null;
        try {
            folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String folderName = "AT_SearchChildren_" + suffix();
            api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, folderName);

            childId = api.createFolder(PROJECT_ID, folderId);
            String childName = "AT_Child_" + suffix();
            api.renameFolder(PROJECT_ID, childId, folderId, childName);

            String resp = api.searchFolderChildren(folderId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询子元素应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            Assertions.assertTrue(resp.contains(childName),
                    "子元素列表应包含子文件夹: " + childName);
            log.info("查询文件夹子元素 通过: folder={}, child={}", folderName, childName);
        } finally {
            if (childId != null) hardCleanFolder(childId);
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询空文件夹子元素(正向)")
    void test_searchFolderChildrenEmpty() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];

            String resp = api.searchFolderChildren(folderId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询空文件夹子元素应成功, resp: " + resp);
            log.info("查询空文件夹子元素 通过: 返回正常");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询文件夹子元素-无效ID(负向)")
    void test_searchFolderChildrenInvalidId() {
        String resp = api.searchFolderChildren("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("查询子元素-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("查询文件夹子元素-空ID(负向)")
    void test_searchFolderChildrenEmptyId() {
        String resp = api.searchFolderChildren("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("查询子元素-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
