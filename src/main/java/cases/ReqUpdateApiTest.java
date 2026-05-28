package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqUpdateApiTest extends BaseTest {

    // ==================== 更新需求列表 ====================

    @Test
    @DisplayName("更新需求列表(正向)")
    void test_updateReqList() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String itemId = api.addReqItem(PROJECT_ID, docId, docId);
            Assertions.assertNotNull(itemId, "新建需求项应返回ID");

            String reqList = "[{\"objectId\":\"" + itemId + "\",\"name\":\"AT_Updated_"
                    + suffix() + "\",\"description\":\"updated by API\"}]";

            String resp = api.updateReqList(docId, reqList);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "更新需求列表应成功, resp: " + resp);
            log.info("更新需求列表 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("更新需求列表-空reqSpeId(负向)")
    void test_updateReqListEmptyReqSpeId() {
        String resp = api.updateReqList("", "[]");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "空reqSpeId应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("更新需求列表-空reqSpeId 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("更新需求列表-无效reqSpeId(负向)")
    void test_updateReqListInvalidReqSpeId() {
        String resp = api.updateReqList("invalid_id_99999", "[]");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "无效reqSpeId应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("更新需求列表-无效reqSpeId 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("更新需求列表-非法JSON(负向)")
    void test_updateReqListMalformedJson() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.updateReqList(docId, "{invalid json}}}");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "非法JSON应被拦截, 实际code=" + code + ", resp: " + resp);
            log.info("更新需求列表-非法JSON 通过: 被拦截, code={}", code);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("更新需求列表-空列表内容(负向)")
    void test_updateReqListEmptyList() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.updateReqList(docId, "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("更新需求列表-空列表: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 编辑描述 ====================

    @Test
    @DisplayName("编辑需求描述(正向)")
    void test_editDescription() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.editDescription(PROJECT_ID, docId, folderId,
                    "AT_Description_" + suffix());

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "编辑描述应成功, resp: " + resp);
            log.info("编辑描述 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("编辑需求描述-空描述(负向)")
    void test_editDescriptionEmpty() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.editDescription(PROJECT_ID, docId, folderId, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("编辑描述-空描述: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("编辑需求描述-超长描述(负向)")
    void test_editDescriptionTooLong() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String longDesc = "D".repeat(5000);
            String resp = api.editDescription(PROJECT_ID, docId, folderId, longDesc);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("编辑描述-超长描述: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("编辑需求描述-无效文档ID(负向)")
    void test_editDescriptionInvalidDocId() {
        String resp = api.editDescription(PROJECT_ID, "invalid_id_99999",
                PROJECT_ID, "测试描述");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "无效文档ID应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("编辑描述-无效文档ID 通过: 被拦截, code={}", code);
    }
}
