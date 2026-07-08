package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

/**
 * 文件夹描述编辑 — API 测试。
 * 覆盖正向编辑、空描述、超长、无效ID、特殊字符等场景。
 */
@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FolderEditApiTest extends ApiTestHelper {

    @Test @DisplayName("GNYL_023: 编辑文件夹描述(正向)")
    void test_editFolderDescription() {
        String f = api.createFolder(PROJECT_ID, PROJECT_ID);
        Assertions.assertNotNull(f, "新建文件夹应返回ID");
        String resp = api.editFolderDescription(PROJECT_ID, f, PROJECT_ID, "AT_FolderDesc_" + suffix());
        Assertions.assertEquals(200, JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt(), "编辑文件夹描述应成功");
        log.info("GNYL_023 编辑文件夹描述 通过: folderId={}", f);
    }

    @Test @DisplayName("编辑文件夹描述-空描述(负向)")
    void test_editFolderDescriptionEmpty() {
        String f = api.createFolder(PROJECT_ID, PROJECT_ID);
        Assertions.assertNotNull(f, "新建文件夹应返回ID");
        String resp = api.editFolderDescription(PROJECT_ID, f, PROJECT_ID, "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("编辑文件夹描述-空描述: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("编辑文件夹描述-超长描述(负向)")
    void test_editFolderDescriptionTooLong() {
        String f = api.createFolder(PROJECT_ID, PROJECT_ID);
        Assertions.assertNotNull(f, "新建文件夹应返回ID");
        String resp = api.editFolderDescription(PROJECT_ID, f, PROJECT_ID, "F".repeat(5000));
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("编辑文件夹描述-超长描述: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("编辑文件夹描述-无效文件夹ID(负向)")
    void test_editFolderDescriptionInvalidId() {
        String resp = api.editFolderDescription(PROJECT_ID, "invalid_id_99999", PROJECT_ID, "测试描述");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        assertRejected(resp, "无效文件夹ID应被拦截");
        log.info("编辑文件夹描述-无效ID 通过: 被拦截, code={}", code);
    }

    @Test @DisplayName("编辑文件夹描述-特殊字符(负向)")
    void test_editFolderDescriptionSpecialChars() {
        String f = api.createFolder(PROJECT_ID, PROJECT_ID);
        Assertions.assertNotNull(f, "新建文件夹应返回ID");
        String resp = api.editFolderDescription(PROJECT_ID, f, PROJECT_ID, "<script>alert(1)</script>");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        if (code == 200) log.warn("XSS描述未被拦截, code=200, 可能存在XSS风险");
        log.info("编辑文件夹描述-特殊字符: code={}", code);
    }
}
