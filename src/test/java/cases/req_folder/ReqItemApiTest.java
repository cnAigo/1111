package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqItemApiTest extends ApiTestHelper {

    // ==================== 新建需求项 ====================

    @Test
    @DisplayName("GNYL_072-R: 需求规格下新建需求项(正向)")
    void test_createReqItem() {
        String folderId = null;
        String docId = null;
        String itemId = null;
        try {
            String[] doc = createTempDoc();
            docId = doc[0];
            folderId = doc[2];

            itemId = api.addReqItem(PROJECT_ID, docId, docId);

            Assertions.assertNotNull(itemId, "新建需求项应返回objectId");
            Assertions.assertFalse(itemId.isEmpty(), "返回的objectId不应为空");

            String resp = api.searchChildReqInfo(docId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询子需求项应成功, resp: " + resp);
            Assertions.assertTrue(resp.contains(itemId),
                    "查询结果应包含新创建的需求项: " + itemId);
            log.info("新建需求项 通过: reqItemId={}, docId={}", itemId, docId);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("新建需求项-空父节点(负向)")
    void test_createReqItemEmptyParent() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.addReqItemRaw(PROJECT_ID, "", docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "空parentId应被拦截");
            log.info("新建需求项-空父节点 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("新建需求项-不存在的文档ID(负向)")
    void test_createReqItemInvalidDoc() {
        String resp = api.addReqItemRaw(PROJECT_ID, "invalid_id_99999", "invalid_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的文档ID应被拦截");
        log.info("新建需求项-无效文档ID 通过: 被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 删除需求项 ====================

    @Test
    @DisplayName("删除需求项(正向)")
    void test_deleteReqItem() {
        String folderId = null;
        String itemId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];
            itemId = api.addReqItem(PROJECT_ID, docId, docId);

            String resp = api.deleteReqItem(itemId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除应成功, resp: " + resp);
            log.info("删除需求项 通过: reqItemId={} 已删除", itemId);
            itemId = null;
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("删除不存在需求项(负向)")
    void test_deleteReqItemInvalid() {
        String resp = api.deleteReqItem("invalid_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "删除不存在的需求项应失败");
        log.info("删除不存在需求项 通过: 被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 恢复需求项 ====================

    @Test
    @DisplayName("恢复已删除需求项(正向)")
    void test_recoverReqItem() {
        String folderId = null;
        String itemId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];
            itemId = api.addReqItem(PROJECT_ID, docId, docId);

            api.deleteReqItem(itemId);

            String resp = api.recoverReqItem(itemId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "恢复应成功, resp: " + resp);

            String childResp = api.searchChildReqInfo(docId);
            JsonObject childRoot = JsonParser.parseString(childResp).getAsJsonObject();
            Assertions.assertEquals(200, childRoot.get("code").getAsInt(),
                    "查询子需求项应成功");
            Assertions.assertTrue(childResp.contains(itemId),
                    "查询结果应包含恢复后的需求项: " + itemId);
            log.info("恢复需求项 通过: reqItemId={} 已恢复", itemId);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== 彻底删除需求项 ====================

    @Test
    @DisplayName("彻底删除需求项(正向)")
    void test_cleanReqItem() {
        String folderId = null;
        String itemId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];
            itemId = api.addReqItem(PROJECT_ID, docId, docId);

            api.deleteReqItem(itemId);
            String resp = api.cleanReqItem(itemId, docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "彻底删除应成功, resp: " + resp);

            String childResp = api.searchChildReqInfo(docId);
            Assertions.assertFalse(childResp.contains(itemId),
                    "彻底删除后不应包含该需求项: " + itemId);
            log.info("彻底删除需求项 通过: reqItemId={} 已彻底删除", itemId);
            itemId = null;
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== 查询子需求项 ====================

    @Test
    @DisplayName("查询需求规格下子需求项(正向)")
    void test_searchChildReqInfo() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String itemId1 = api.addReqItem(PROJECT_ID, docId, docId);
            String itemId2 = api.addReqItem(PROJECT_ID, docId, docId);

            String resp = api.searchChildReqInfo(docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询应成功, resp: " + resp);
            Assertions.assertTrue(root.has("data"), "应有data字段");
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            Assertions.assertTrue(resp.contains(itemId1),
                    "结果应包含第1个需求项: " + itemId1);
            Assertions.assertTrue(resp.contains(itemId2),
                    "结果应包含第2个需求项: " + itemId2);
            log.info("查询子需求项 通过: docId={} 包含 {} 和 {}", docId, itemId1, itemId2);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询子需求项-空文档(正向)")
    void test_searchChildReqInfoEmpty() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchChildReqInfo(docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "空文档查询应成功, resp: " + resp);
            log.info("查询空文档子需求项 通过: 返回正常");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }
}
