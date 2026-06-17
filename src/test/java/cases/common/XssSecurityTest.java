package cases.common;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Tag("CommonModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class XssSecurityTest extends ApiTestHelper {

    private JsonArray payloads;

    @BeforeAll
    void loadPayloads() {
        try (var r = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("xss-payloads.json"),
                StandardCharsets.UTF_8)) {
            payloads = JsonParser.parseReader(r).getAsJsonArray();
            log.info("加载 {} 个 XSS payload", payloads.size());
        } catch (Exception e) {
            payloads = new JsonArray();
            log.error("加载 payload 失败: {}", e.getMessage());
        }
    }

    // ═══════════════════════ 文件夹名称 ═══════════════════════

    @Test @Order(1)
    @DisplayName("XSS-文件夹新建")
    void test_xss_folderCreate() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String resp = api.renameFolder(PROJECT_ID, folderId, PROJECT_ID, "AT_" + payload);
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 文件夹新建未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("文件夹新建: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(文件夹新建)");
    }

    @Test @Order(2)
    @DisplayName("XSS-文件夹重命名")
    void test_xss_folderRename() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String[] f = createTempFolder();
            String resp = api.renameFolder(PROJECT_ID, f[0], PROJECT_ID, "AT_" + payload);
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 文件夹重命名未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("文件夹重命名: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(文件夹重命名)");
    }

    // ═══════════════════════ 需求规格名称 ═══════════════════════

    @Test @Order(3)
    @DisplayName("XSS-需求规格新建")
    void test_xss_docCreate() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String[] doc = createTempDoc();
            String resp = api.renameDocument(PROJECT_ID, doc[0], doc[2], "AT_" + payload);
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 需求规格新建未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("需求规格新建: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(需求规格新建)");
    }

    @Test @Order(4)
    @DisplayName("XSS-需求规格重命名")
    void test_xss_docRename() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String[] doc = createTempDoc();
            String resp = api.renameDocument(PROJECT_ID, doc[0], doc[2], "AT_" + payload);
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 需求规格重命名未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("需求规格重命名: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(需求规格重命名)");
    }

    // ═══════════════════════ 描述字段 ═══════════════════════

    @Test @Order(5)
    @DisplayName("XSS-需求规格描述")
    void test_xss_docDescription() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String[] doc = createTempDoc();
            String resp = api.editDescription(PROJECT_ID, doc[0], doc[2], payload);
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 描述未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("描述字段: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(描述)");
    }

    // ═══════════════════════ 需求项内容 ═══════════════════════

    @Test @Order(6)
    @DisplayName("XSS-需求项更新")
    void test_xss_reqItemUpdate() {
        int blocked = 0, leaked = 0;
        for (int i = 0; i < payloads.size(); i++) {
            JsonObject p = payloads.get(i).getAsJsonObject();
            String id = p.get("id").getAsString();
            String payload = p.get("payload").getAsString();
            String desc = p.get("desc").getAsString();

            String[] doc = createTempDoc();
            String itemId = api.addReqItem(PROJECT_ID, doc[0], doc[0]);

            String childResp = api.searchChildReqInfo(doc[0]);
            JsonObject childRoot = JsonParser.parseString(childResp).getAsJsonObject();
            if (childRoot.get("code").getAsInt() != 200) continue;

            var data = childRoot.getAsJsonObject("data");
            var children = data.getAsJsonArray("children");
            if (children == null || children.size() == 0) continue;

            JsonObject item = children.get(0).getAsJsonObject();
            item.addProperty("description", payload);
            String resp = api.updateReqList(doc[0], "[" + item.toString() + "]");
            int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();

            if (code == 200) {
                log.error("XSS泄露 [{}] {}: 需求项更新未拦截 payload={}", id, desc, payload);
                leaked++;
            } else {
                blocked++;
                log.info("  ✓ [{}] {} 被拦截 code={}", id, desc, code);
            }
        }
        log.info("需求项更新: 拦截{}/{} 泄露{}", blocked, payloads.size(), leaked);
        Assertions.assertEquals(0, leaked, "XSS泄露: " + leaked + " 个payload未被拦截(需求项)");
    }

    // ═══════════════════════ 汇总 ═══════════════════════

    @Test @Order(99)
    @DisplayName("XSS-汇总")
    void test_xss_summary() {
        log.info("====== XSS安全测试完成 ======");
    }
}
