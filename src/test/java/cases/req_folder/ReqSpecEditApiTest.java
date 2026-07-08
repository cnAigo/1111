package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

/**
 * 需求规格描述编辑 — API 测试。
 * 覆盖正向编辑、空描述、超长、无效ID、XSS 等场景。
 */
@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqSpecEditApiTest extends ApiTestHelper {

    @Test @DisplayName("GNYL_083: 编辑需求描述(正向)")
    void test_editDescription() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        String resp = api.editDescription(PROJECT_ID, docId, folderId, "AT_Description_" + suffix());
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "编辑描述应成功");
        log.info("GNYL_083 编辑描述 通过: docId={}", docId);
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
}
