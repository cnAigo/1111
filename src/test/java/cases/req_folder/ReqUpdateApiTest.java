package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqUpdateApiTest extends ApiTestHelper {

    @Test @DisplayName("更新需求列表(正向)")
    void test_updateReqList() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String itemId = api.addReqItem(PROJECT_ID, docId, docId);
        Assertions.assertNotNull(itemId, "新建需求项应返回ID");
        String childResp = api.searchChildReqInfo(docId);
        JsonObject childRoot = JsonParser.parseString(childResp).getAsJsonObject();
        Assertions.assertEquals(200, childRoot.get("code").getAsInt(), "查询子需求信息应成功");
        var data = childRoot.getAsJsonObject("data");
        var children = data.getAsJsonArray("children");
        Assertions.assertTrue(children.size() > 0, "至少应有一条需求项");
        JsonObject item = children.get(0).getAsJsonObject();
        item.addProperty("description", "<p>AT_Updated_" + suffix() + "</p>");
        String resp = api.updateReqList(docId, "[" + item.toString() + "]");
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "更新需求列表应成功");
        log.info("更新需求列表 通过: docId={}", docId);
    }

    @Test @DisplayName("更新需求列表-空reqSpeId(负向)")
    void test_updateReqListEmptyReqSpeId() {
        String resp = api.updateReqList("", "[]");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "空reqSpeId应被拦截");
        log.info("更新需求列表-空reqSpeId 通过: 被拦截, code={}", code);
    }

    @Test @DisplayName("更新需求列表-无效reqSpeId(负向)")
    void test_updateReqListInvalidReqSpeId() {
        String resp = api.updateReqList("invalid_id_99999", "[]");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "无效reqSpeId应被拦截");
        log.info("更新需求列表-无效reqSpeId 通过: 被拦截, code={}", code);
    }

    @Test @DisplayName("更新需求列表-非法JSON(负向)")
    void test_updateReqListMalformedJson() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String resp = api.updateReqList(docId, "{invalid json}}}");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "非法JSON应被拦截");
        log.info("更新需求列表-非法JSON 通过: 被拦截, code={}", code);
    }

    @Test @DisplayName("更新需求列表-空列表内容(负向)")
    void test_updateReqListEmptyList() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String resp = api.updateReqList(docId, "[]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("更新需求列表-空列表: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("编辑需求描述(正向)")
    void test_editDescription() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        String resp = api.editDescription(PROJECT_ID, docId, folderId, "AT_Description_" + suffix());
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "编辑描述应成功");
        log.info("编辑描述 通过: docId={}", docId);
    }

    @Test @DisplayName("编辑需求描述-空描述(负向)")
    void test_editDescriptionEmpty() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        String resp = api.editDescription(PROJECT_ID, docId, folderId, "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("编辑描述-空描述: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("编辑需求描述-超长描述(负向)")
    void test_editDescriptionTooLong() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        String resp = api.editDescription(PROJECT_ID, docId, folderId, "D".repeat(5000));
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("编辑描述-超长描述: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("编辑需求描述-无效文档ID(负向)")
    void test_editDescriptionInvalidDocId() {
        String resp = api.editDescription(PROJECT_ID, "invalid_id_99999", PROJECT_ID, "测试描述");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "无效文档ID应被拦截");
        log.info("编辑描述-无效文档ID 通过: 被拦截, code={}", code);
    }

    @Test @DisplayName("编辑需求描述-特殊字符XSS(负向)")
    void test_editDescriptionSpecialChars() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        String resp = api.editDescription(PROJECT_ID, docId, folderId, "<img src=x onerror=alert(1)>");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        if (code == 200) log.warn("XSS描述未被拦截, code=200, 可能存在XSS风险");
        log.info("编辑描述-特殊字符XSS: code={}", code);
    }

    @Test @DisplayName("删除需求项(正向)")
    void test_deleteReqItem() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String itemId = api.addReqItem(PROJECT_ID, docId, docId);
        Assertions.assertNotNull(itemId, "新建需求项应返回ID");
        String resp = api.deleteReqItem(itemId);
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "删除需求项应成功");
        api.cleanReqItem(itemId, docId);
        log.info("删除需求项 通过: itemId={}", itemId);
    }

    @Test @DisplayName("恢复需求项(正向)")
    void test_recoverReqItem() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String itemId = api.addReqItem(PROJECT_ID, docId, docId);
        Assertions.assertNotNull(itemId, "新建需求项应返回ID");
        api.deleteReqItem(itemId);
        String resp = api.recoverReqItem(itemId);
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "恢复需求项应成功");
        api.deleteReqItem(itemId);
        api.cleanReqItem(itemId, docId);
        log.info("恢复需求项 通过: itemId={}", itemId);
    }

    @Test @DisplayName("删除不存在的需求项(负向)")
    void test_deleteNonExistingReqItem() {
        String resp = api.deleteReqItem("nonexistent_id_99999");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "删除不存在的需求项应失败");
        log.info("删除不存在的需求项 通过: code={}", code);
    }

    @Test @DisplayName("恢复不存在的需求项(负向)")
    void test_recoverNonExistingReqItem() {
        String resp = api.recoverReqItem("nonexistent_id_99999");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "恢复不存在的需求项应失败");
        log.info("恢复不存在的需求项 通过: code={}", code);
    }

    @Test @DisplayName("查询需求项子信息(正向)")
    void test_searchChildReqInfo() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String itemId = api.addReqItem(PROJECT_ID, docId, docId);
        Assertions.assertNotNull(itemId, "新建需求项应返回ID");
        String resp = api.searchChildReqInfo(docId);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "查询需求项子信息应成功");
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("查询需求项子信息 通过: docId={}", docId);
    }

    @Test @DisplayName("查询需求项子信息-无效ID(负向)")
    void test_searchChildReqInfoInvalidId() {
        String resp = api.searchChildReqInfo("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("查询需求项子信息-无效ID: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("更新需求列表-不存在的itemId(负向)")
    void test_updateReqListInvalidItemId() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String resp = api.updateReqList(docId, "[{\"objectId\":\"nonexistent_id_99999\",\"name\":\"test\"}]");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "不存在的itemId应被拦截");
        log.info("更新需求列表-无效itemId 通过: code={}", code);
    }

    @Test @DisplayName("更新需求列表-空item对象(负向)")
    void test_updateReqListEmptyItemObject() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String resp = api.updateReqList(docId, "[{}]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("更新需求列表-空item对象: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
