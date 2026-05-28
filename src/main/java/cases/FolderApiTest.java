package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FolderApiTest extends BaseTest {

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
                Assertions.fail("后端未拦截超长名称(200字符), code=200, 疑似缺陷");
            }
            log.info("GNYL_022-L 通过: 超长名称被拦截, code={}", code);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
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
}
