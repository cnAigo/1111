package cases.user_manage;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.*;

@Tag("UserManageModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SystemPostApiTest extends ApiTestHelper {

    // ==================== 岗位列表 ====================

    @Test
    @DisplayName("QTYL_POST_001: 查询岗位列表(正向)")
    void test_sysPostList() {
        String resp = api.sysPostList(1, 10, "", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "查询岗位列表应成功, resp: " + resp);
        Assertions.assertTrue(root.has("rows"), "应包含rows字段");
        log.info("QTYL_POST_001 通过: total={}", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_POST_002: 按名称过滤(正向)")
    void test_sysPostListByName() {
        String resp = api.sysPostList(1, 10, "", "管理", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按名称过滤应成功");
        log.info("QTYL_POST_002 通过: total={}", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_POST_003: 按状态过滤(正向)")
    void test_sysPostListByStatus() {
        String resp = api.sysPostList(1, 10, "", "", "0");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按状态过滤应成功");
        log.info("QTYL_POST_003 通过: total={}", root.get("total").getAsInt());
    }

    // ==================== CRUD完整流程 ====================

    @Test
    @DisplayName("QTYL_POST_004: 岗位CRUD完整流程(创建→查看→修改→删除)")
    void test_sysPostFullCrud() {
        String postName = "ATP_" + suffix();
        String postCode = "ATPC" + suffix();
        String postId = null;
        try {
            // 1. Create
            String createResp = api.sysPostCreate(postName, postCode, 1, "0", "自动化测试");
            JsonObject createRoot = JsonParser.parseString(createResp).getAsJsonObject();
            Assertions.assertEquals(200, createRoot.get("code").getAsInt(),
                    "创建岗位应成功, resp: " + createResp);

            // 2. Find post ID from list
            String listResp = api.sysPostList(1, 20, postCode, "", "");
            JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
            if (listRoot.has("rows") && !listRoot.get("rows").isJsonNull()) {
                var rows = listRoot.getAsJsonArray("rows");
                if (rows.size() > 0) {
                    postId = rows.get(0).getAsJsonObject().get("postId").getAsString();
                }
            }
            Assertions.assertNotNull(postId, "应能查到刚创建的岗位ID");

            // 3. Get by ID
            String getResp = api.sysPostGetById(postId);
            JsonObject getRoot = JsonParser.parseString(getResp).getAsJsonObject();
            Assertions.assertEquals(200, getRoot.get("code").getAsInt(),
                    "查看岗位应成功");
            Assertions.assertEquals(postName,
                    getRoot.getAsJsonObject("data").get("postName").getAsString(),
                    "岗位名称应一致");

            // 4. Update
            JsonObject postData = getRoot.getAsJsonObject("data");
            postData.addProperty("postName", postName + "_改");
            postData.addProperty("remark", "修改测试");
            String updateResp = api.sysPostUpdate(postData.toString());
            JsonObject updateRoot = JsonParser.parseString(updateResp).getAsJsonObject();
            Assertions.assertEquals(200, updateRoot.get("code").getAsInt(),
                    "修改岗位应成功, resp: " + updateResp);
            log.info("岗位CRUD完成: postId={}", postId);
        } finally {
            if (postId != null) {
                api.sysPostDelete(postId);
                log.info("已清理测试岗位: postId={}", postId);
            }
        }
    }

    // ==================== 导出 ====================

    @Test
    @DisplayName("QTYL_POST_005: 导出岗位(正向)")
    void test_sysPostExport() {
        APIResponse response = api.sysPostExport();
        Assertions.assertEquals(200, response.status(),
                "导出应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "导出文件不应为空");
        log.info("QTYL_POST_005 通过: 导出文件大小={} bytes", body.length);
    }

    // ==================== 负向测试 ====================

    @Test
    @DisplayName("QTYL_POST_006: 创建岗位-空名称(负向)")
    void test_sysPostCreateEmptyName() {
        String resp = api.sysPostCreate("", "ATPC" + suffix(), 1, "0", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_POST_006: 空名称 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_POST_007: 创建岗位-空编码(负向)")
    void test_sysPostCreateEmptyCode() {
        String resp = api.sysPostCreate("ATP_" + suffix(), "", 1, "0", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_POST_007: 空编码 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_POST_008: 创建岗位-重复编码(负向)")
    void test_sysPostCreateDuplicateCode() {
        String postCode = "ATDUP" + suffix();
        api.sysPostCreate("ATPost1_" + suffix(), postCode, 1, "0", "");

        try {
            String resp = api.sysPostCreate("ATPost2_" + suffix(), postCode, 1, "0", "");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("QTYL_POST_008: 重复编码 code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            cleanupPostByCode(postCode);
        }
    }

    @Test
    @DisplayName("QTYL_POST_009: 查看岗位-无效ID(负向)")
    void test_sysPostGetInvalidId() {
        String resp = api.sysPostGetById("99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_POST_009: 无效ID code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_POST_010: 删除岗位-无效ID(负向)")
    void test_sysPostDeleteInvalidId() {
        String resp = api.sysPostDelete("99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_POST_010: 无效删除 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_POST_011: 删除岗位-空ID(负向)")
    void test_sysPostDeleteEmptyId() {
        String resp = api.sysPostDelete("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_POST_011: 空ID code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ========== helpers ==========

    private void cleanupPostByCode(String postCode) {
        try {
            String listResp = api.sysPostList(1, 10, postCode, "", "");
            JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
            if (listRoot.has("rows") && !listRoot.get("rows").isJsonNull()) {
                var rows = listRoot.getAsJsonArray("rows");
                for (int i = 0; i < rows.size(); i++) {
                    api.sysPostDelete(rows.get(i).getAsJsonObject().get("postId").getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
