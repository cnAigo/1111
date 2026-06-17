package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

/**
 * 批量更新需求列表 — API 测试。
 * 覆盖正向更新、各种负向参数校验。
 */
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
