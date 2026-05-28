package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqSpecApiTest extends BaseTest {

    // ==================== 新建需求规格 ====================

    @Test
    @DisplayName("GNYL_072: 文件夹下新建需求规格(正向)")
    void test_GNYL_072_createDocUnderFolder() {
        String folderId = null;
        String docId = null;
        String docName = null;
        try {
            String[] doc = createTempDoc();
            docId = doc[0];
            docName = doc[1];
            folderId = doc[2];

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(docName),
                    "树结构中应包含新建的需求规格: " + docName);
            log.info("GNYL_072 通过: 新建需求规格 [{}] docId={}", docName, docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_073: 根节点下新建需求规格(正向)")
    void test_GNYL_073_createDocUnderRoot() {
        String docId = null;
        String docName = null;
        try {
            String[] doc = createTempDocFull(PROJECT_ID);
            docId = doc[0];
            docName = doc[1];

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(docName),
                    "树结构中应包含新建的需求规格: " + docName);
            log.info("GNYL_073 通过: 根节点下新建需求规格 [{}] docId={}", docName, docId);
        } finally {
            if (docId != null) cleanupDoc(docId, PROJECT_ID);
        }
    }

    @Test
    @DisplayName("GNYL_073-N: 新建需求规格-空名称(负向)")
    void test_GNYL_073N_createDocEmptyName() {
        String docId = null;
        try {
            docId = api.createDocument(PROJECT_ID, PROJECT_ID);
            String resp = api.renameDocument(PROJECT_ID, docId, PROJECT_ID, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "空名称应被拦截, 实际code=" + code + ", resp: " + resp);
            log.info("GNYL_073-N 通过: 空名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (docId != null) cleanupDoc(docId, PROJECT_ID);
        }
    }

    // ==================== 修改需求规格名称 ====================

    @Test
    @DisplayName("GNYL_078: 修改需求规格名称(正向)")
    void test_GNYL_078_renameDocValid() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            String oldName = doc[1];
            folderId = doc[2];

            String newName = "AT_DocRenamed_" + suffix();
            String resp = api.renameDocument(PROJECT_ID, docId, folderId, newName);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "重命名应成功, resp: " + resp);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(newName),
                    "树结构中应包含新名称: " + newName);
            Assertions.assertFalse(treeResp.contains(oldName),
                    "树结构中不应包含旧名称: " + oldName);
            log.info("GNYL_078 通过: 重命名 {} -> {}", oldName, newName);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_080: 修改为重复的需求规格名称(负向)")
    void test_GNYL_080_renameDocDuplicate() {
        String folderId = null;
        try {
            folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
            api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, "AT_Folder_" + suffix());

            String[] doc1 = createTempDocFull(folderId);
            String docId1 = doc1[0];
            String name1 = doc1[1];

            String[] doc2 = createTempDocFull(folderId);
            String docId2 = doc2[0];

            String resp = api.renameDocument(PROJECT_ID, docId2, folderId, name1);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "重命名为重复名称应失败, 实际code=" + code + ", resp: " + resp);
            log.info("GNYL_080 通过: 重复名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_082: 需求规格名称为空(负向)")
    void test_GNYL_082_renameDocEmptyName() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            String oldName = doc[1];
            folderId = doc[2];

            String resp = api.renameDocument(PROJECT_ID, docId, folderId, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                api.renameDocument(PROJECT_ID, docId, folderId, oldName);
                Assertions.fail("后端未拦截空名称, code=200, 疑似缺陷");
            }
            log.info("GNYL_082 通过: 空名称被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_082-L: 需求规格名称超长(负向)")
    void test_GNYL_082L_renameDocTooLong() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            String oldName = doc[1];
            folderId = doc[2];

            String longName = "A".repeat(200);
            String resp = api.renameDocument(PROJECT_ID, docId, folderId, longName);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                api.renameDocument(PROJECT_ID, docId, folderId, oldName);
                Assertions.fail("后端未拦截超长名称(200字符), code=200, 疑似缺陷");
            }
            log.info("GNYL_082-L 通过: 超长名称被拦截, code={}", code);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 删除需求规格 ====================

    @Test
    @DisplayName("GNYL_084: 删除需求规格(正向)")
    void test_GNYL_084_deleteDoc() {
        String folderId = null;
        String docId = null;
        try {
            String[] doc = createTempDoc();
            docId = doc[0];
            String docName = doc[1];
            folderId = doc[2];

            String resp = api.deleteDocument(docId, folderId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除应成功, resp: " + resp);
            log.info("GNYL_084 通过: 删除需求规格 [{}] 成功", docName);
            docId = null;
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_086: 恢复需求规格(正向)")
    void test_GNYL_086_recoverDoc() {
        String folderId = null;
        String docId = null;
        String docName = null;
        try {
            String[] doc = createTempDoc();
            docId = doc[0];
            docName = doc[1];
            folderId = doc[2];

            api.deleteDocument(docId, folderId);

            String resp = api.recoverDocument(docId, folderId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "恢复应成功, resp: " + resp);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertTrue(treeResp.contains(docName),
                    "恢复后树结构中应包含该需求规格: " + docName);
            log.info("GNYL_086 通过: 恢复需求规格 [{}] 成功", docName);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("GNYL_088: 彻底删除需求规格(正向)")
    void test_GNYL_088_forceCleanDoc() {
        String docId = null;
        String docName = null;
        try {
            String[] doc = createTempDocFull(PROJECT_ID);
            docId = doc[0];
            docName = doc[1];

            api.deleteDocument(docId, PROJECT_ID);
            api.forceCleanDocument(docId, PROJECT_ID);

            String treeResp = api.getTree(PROJECT_ID, PROJECT_ID);
            Assertions.assertFalse(treeResp.contains(docName),
                    "彻底删除后树结构中不应包含该需求规格: " + docName);
            log.info("GNYL_088 通过: 彻底删除需求规格 [{}] 成功", docName);
            docId = null;
        } finally {
            if (docId != null) cleanupDoc(docId, PROJECT_ID);
        }
    }
}
