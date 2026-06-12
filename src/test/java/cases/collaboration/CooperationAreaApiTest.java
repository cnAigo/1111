package cases.collaboration;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("CollaborationModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CooperationAreaApiTest extends ApiTestHelper {

    // ── helpers ──
    private static String idOf(JsonObject data) {
        if (data == null) return null;
        if (data.has("id")) return data.get("id").getAsString();
        if (data.has("objectId")) return data.get("objectId").getAsString();
        return null;
    }

    // ==================== 新建合作区 ====================

    @Test
    @DisplayName("QTYL_002: 新建合作区(正向)")
    void test_QTYL_002_addCooperationArea() {
        String code = "ATCA" + suffix();
        String name = "AT合作区_" + suffix();
        String resp = api.addCooperationArea(name, code, "内部", "自动化测试");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "新建合作区应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        // Cleanup
        if (root.has("data") && !root.get("data").isJsonNull()) {
            String id = idOf(root.getAsJsonObject("data"));
            api.deleteCooperationArea(id);
        }
        log.info("QTYL_002 通过: 新建合作区 code={}", code);
    }

    @Test
    @DisplayName("QTYL_004: 合作区名称为空(负向)")
    void test_QTYL_004_addEmptyName() {
        String resp = api.addCooperationArea("", "ATCE" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空名称应被拦截");
        log.info("QTYL_004 通过: 空名称被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_005: 合作区编码为空(负向)")
    void test_QTYL_005_addEmptyCode() {
        String resp = api.addCooperationArea("AT合作区_" + suffix(), "", "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空编码应被拦截");
        log.info("QTYL_005 通过: 空编码被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_006: 密级未选(负向)")
    void test_QTYL_006_addEmptySecurityLevel() {
        String resp = api.addCooperationArea("AT合作区_" + suffix(), "ATCS" + suffix(), "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空密级应被拦截");
        log.info("QTYL_006 通过: 空密级被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_007: 编码必须以字母开头(负向)")
    void test_QTYL_007_codeMustStartWithLetter() {
        String resp = api.addCooperationArea("AT合作区_" + suffix(), "123ABC", "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "数字开头编码应被拦截");
        log.info("QTYL_007 通过: 数字开头编码被拦截, code={}", code);
    }

    @Test
    @DisplayName("QTYL_008: 编码含特殊字符(负向)")
    void test_QTYL_008_codeSpecialChars() {
        String resp = api.addCooperationArea("AT合作区_" + suffix(), "AT@#$", "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "特殊字符编码应被拦截");
        log.info("QTYL_008 通过: 特殊字符编码被拦截, code={}", code);
    }

    @Test
    @DisplayName("QTYL_009: 名称含空格(负向)")
    void test_QTYL_009_nameWithSpace() {
        String resp = api.addCooperationArea("AT 合作区_" + suffix(), "ATCW" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "含空格名称应被拦截");
        log.info("QTYL_009 通过: 含空格名称被拦截, code={}", code);
    }

    @Test
    @DisplayName("QTYL_010: 名称重复(负向)")
    void test_QTYL_010_duplicateName() {
        String name = "ATDup_" + suffix();
        String code1 = "ATCD1" + suffix();
        api.addCooperationArea(name, code1, "内部", "");
        String resp = api.addCooperationArea(name, "ATCD2" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "重复名称应被拦截");
        // Cleanup: find and delete the first area
        cleanupCoopAreaByName(name);
        log.info("QTYL_010 通过: 重复名称被拦截, code={}", code);
    }

    @Test
    @DisplayName("QTYL_011: 编码重复(负向)")
    void test_QTYL_011_duplicateCode() {
        String code = "ATCDD" + suffix();
        api.addCooperationArea("ATDupName1_" + suffix(), code, "内部", "");
        String resp = api.addCooperationArea("ATDupName2_" + suffix(), code, "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int c = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, c,
                "重复编码应被拦截, 实际code=" + c + ", resp: " + resp);
        // Cleanup
        String searchResp = api.searchCooperationAreaList("", code);
        JsonObject sr = JsonParser.parseString(searchResp).getAsJsonObject();
        if (sr.has("data") && sr.get("data").isJsonArray()) {
            var arr = sr.getAsJsonArray("data");
            for (var el : arr) {
                JsonObject item = el.getAsJsonObject();
                if (code.equals(item.get("name").getAsString())) {
                    api.deleteCooperationArea(idOf(item));
                    break;
                }
            }
        }
        log.info("QTYL_011 通过: 重复编码被拦截, code={}", c);
    }

    @Test
    @DisplayName("QTYL_010-L: 名称超长(负向)")
    void test_QTYL_010L_nameTooLong() {
        String longName = "A".repeat(200);
        String resp = api.addCooperationArea(longName, "ATCL" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_010-L 超长名称: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 绕过命名规则 ====================

    @Test
    @DisplayName("名称含Unicode特殊字符(尝试绕过)")
    void test_nameWithUnicodeChars() {
        String resp = api.addCooperationArea("AT测试 名称_" + suffix(),
                "ATUNI" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("Unicode特殊字符: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) {
            log.warn("Unicode特殊字符未被拦截, 可能存在绕过风险");
            if (root.has("data") && !root.get("data").isJsonNull())
                api.deleteCooperationArea(idOf(root.getAsJsonObject("data")));
        }
    }

    @Test
    @DisplayName("名称含HTML标签(尝试XSS)")
    void test_nameWithXss() {
        String xssName = "AT<script>alert(1)</script>_" + suffix();
        String resp = api.addCooperationArea(xssName, "ATXS1" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code == 200) {
            log.warn("XSS名称未被拦截, code=200, 可能存在XSS风险");
            if (root.has("data") && !root.get("data").isJsonNull())
                api.deleteCooperationArea(idOf(root.getAsJsonObject("data")));
        }
        log.info("XSS名称: code={}", code);
    }

    @Test
    @DisplayName("名称含SQL注入字符(尝试注入)")
    void test_nameWithSqlInjection() {
        String resp = api.addCooperationArea("AT'; DROP TABLE coop;--_" + suffix(),
                "ATSQL" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "SQL注入不应导致500, resp: " + resp);
        log.info("SQL注入名称: code={}", code);
    }

    @Test
    @DisplayName("编码含下划线(尝试绕过)")
    void test_codeWithUnderscore() {
        String resp = api.addCooperationArea("AT下划线_" + suffix(),
                "AT_CD" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("编码含下划线: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200 && root.has("data") && !root.get("data").isJsonNull())
            api.deleteCooperationArea(idOf(root.getAsJsonObject("data")));
    }

    @Test
    @DisplayName("编码含中文(尝试绕过)")
    void test_codeWithChinese() {
        String resp = api.addCooperationArea("AT中文编码_" + suffix(),
                "AT中文" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("编码含中文: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200 && root.has("data") && !root.get("data").isJsonNull())
            api.deleteCooperationArea(idOf(root.getAsJsonObject("data")));
    }

    @Test
    @DisplayName("名称仅含数字(尝试绕过)")
    void test_nameDigitsOnly() {
        String resp = api.addCooperationArea("1234567890", "ATDIG" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("纯数字名称: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200 && root.has("data") && !root.get("data").isJsonNull())
            api.deleteCooperationArea(idOf(root.getAsJsonObject("data")));
    }

    // ==================== 修改合作区 ====================

    @Test
    @DisplayName("QTYL_003: 修改合作区(正向)")
    void test_QTYL_003_updateCooperationArea() {
        String code = "ATCU" + suffix();
        String addResp = api.addCooperationArea("AT原始名称_" + suffix(), code, "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建应成功");
        String id = idOf(addRoot.getAsJsonObject("data"));

        String newName = "AT修改后_" + suffix();
        String resp = api.updateCooperationArea(id, newName, code, "秘密", "修改测试");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "修改应成功, resp: " + resp);
        // Cleanup
        api.deleteCooperationArea(id);
        log.info("QTYL_003 通过: 修改合作区 id={}, newName={}", id, newName);
    }

    @Test
    @DisplayName("修改合作区-不存在ID(负向)")
    void test_updateCooperationAreaInvalidId() {
        String resp = api.updateCooperationArea("invalid_id_99999", "测试", "ATCX" + suffix(),
                "内部", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的ID应被拦截");
        log.info("修改合作区-无效ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("修改合作区-空名称(负向)")
    void test_updateCooperationAreaEmptyName() {
        String code = "ATCEU" + suffix();
        String addResp = api.addCooperationArea("AT更新测试_" + suffix(), code, "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        String id = null;
        if (addRoot.get("code").getAsInt() == 200 && addRoot.has("data")
                && !addRoot.get("data").isJsonNull()) {
            id = idOf(addRoot.getAsJsonObject("data"));
            String resp = api.updateCooperationArea(id, "", code, "内部", "");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int c = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, c,
                    "空名称应被拦截, 实际code=" + c + ", resp: " + resp);
            log.info("修改合作区-空名称 通过: 被拦截, code={}", c);
        }
        if (id != null) api.deleteCooperationArea(id);
    }

    @Test
    @DisplayName("修改合作区-尝试改为已存在的名称(负向)")
    void test_updateToExistingName() {
        String name1 = "AT_Exist1_" + suffix();
        String name2 = "AT_Exist2_" + suffix();
        String addResp1 = api.addCooperationArea(name1, "ATCE1" + suffix(), "内部", "");
        String addResp2 = api.addCooperationArea(name2, "ATCE2" + suffix(), "内部", "");
        JsonObject r1 = JsonParser.parseString(addResp1).getAsJsonObject();
        JsonObject r2 = JsonParser.parseString(addResp2).getAsJsonObject();
        String id1 = null, id2 = null;
        if (r1.get("code").getAsInt() == 200 && r1.has("data") && !r1.get("data").isJsonNull())
            id1 = idOf(r1.getAsJsonObject("data"));
        if (r2.get("code").getAsInt() == 200 && r2.has("data") && !r2.get("data").isJsonNull())
            id2 = idOf(r2.getAsJsonObject("data"));

        if (id1 != null && id2 != null) {
            // Try to rename area2 to area1's name
            String resp = api.updateCooperationArea(id2, name1, "ATCE2" + suffix(), "内部", "");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "更名为已存在名称应被拦截");
            log.info("更名为已存在名称: 被拦截, code={}", code);
        }
        if (id1 != null) api.deleteCooperationArea(id1);
        if (id2 != null) api.deleteCooperationArea(id2);
    }

    // ==================== 删除合作区 ====================

    @Test
    @DisplayName("QTYL_013: 删除无用户合作区(正向)")
    void test_QTYL_013_deleteCooperationAreaWithoutUsers() {
        String addResp = api.addCooperationArea("AT待删除_" + suffix(), "ATCDL" + suffix(),
                "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建应成功");
        String id = idOf(addRoot.getAsJsonObject("data"));

        String resp = api.deleteCooperationArea(id);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "删除应成功, resp: " + resp);
        log.info("QTYL_013 通过: 删除合作区 id={}", id);
    }

    @Test
    @DisplayName("删除有用户合作区(尝试删发布的)")
    void test_deleteCooperationAreaWithUsers() {
        String addResp = api.addCooperationArea("AT有用户_" + suffix(), "ATCDU2" + suffix(),
                "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建应成功");
        String id = idOf(addRoot.getAsJsonObject("data"));

        // Add a user to the area
        api.addCooperationAreaUser(id, "admin");

        // Try to delete the area that now has users (this should fail or need special handling)
        String resp = api.deleteCooperationArea(id);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("删除有用户合作区: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");

        // Cleanup: remove user first, then delete
        api.deleteCooperationAreaUser(id, "admin");
        api.deleteCooperationArea(id);
    }

    @Test
    @DisplayName("删除合作区-不存在ID(负向)")
    void test_deleteCooperationAreaInvalidId() {
        String resp = api.deleteCooperationArea("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的ID应被拦截");
        log.info("删除合作区-无效ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("删除合作区-空ID(负向)")
    void test_deleteCooperationAreaEmptyId() {
        String resp = api.deleteCooperationArea("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空ID应被拦截");
        log.info("删除合作区-空ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("删除合作区-SQL注入ID(负向)")
    void test_deleteCooperationAreaSqlInjection() {
        String resp = api.deleteCooperationArea("' OR '1'='1");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "SQL注入ID不应导致500, resp: " + resp);
        log.info("删除合作区-SQL注入: code={}", code);
    }

    // ==================== 搜索合作区 ====================

    @Test
    @DisplayName("QTYL_014: 搜索存在的合作区(正向)")
    void test_QTYL_014_searchCooperationAreaExists() {
        String name = "AT可搜索_" + suffix();
        String addResp = api.addCooperationArea(name, "ATCSR" + suffix(), "内部", "");

        String resp = api.searchCooperationAreaList(name, "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        Assertions.assertTrue(resp.contains(name),
                "搜索结果应包含合作区: " + name);
        // Cleanup
        cleanupCoopAreaByName(name);
        log.info("QTYL_014 通过: 搜索到合作区 [{}]", name);
    }

    @Test
    @DisplayName("搜索不存在的合作区(负向)")
    void test_searchCooperationAreaNotExists() {
        String resp = api.searchCooperationAreaList("NonExistent_" + suffix(), "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索请求本身应成功");
        Assertions.assertTrue(api.isDataEmpty(resp),
                "不存在的合作区应返回空data");
        log.info("搜索不存在合作区 通过: data为空");
    }

    @Test
    @DisplayName("搜索合作区-空关键字(负向)")
    void test_searchCooperationAreaEmptyKeyword() {
        String resp = api.searchCooperationAreaList("", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索合作区-空关键字: code={}, dataSize={}",
                code, root.has("data") && !root.get("data").isJsonNull()
                        ? root.getAsJsonArray("data").size() : "null");
    }

    @Test
    @DisplayName("搜索合作区-SQL注入防护(负向)")
    void test_searchCooperationAreaSqlInjection() {
        String resp = api.searchCooperationAreaList("'; DROP TABLE coop;--", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "SQL注入不应导致500, resp: " + resp);
        log.info("搜索合作区-SQL注入 通过: code={}", code);
    }

    // ==================== 合作区用户管理 ====================

    @Test
    @DisplayName("添加合作区用户(正向)")
    void test_addCooperationAreaUser() {
        String name = "AT_AreaUser_" + suffix();
        String code = "ATCAU" + suffix();
        String addResp = api.addCooperationArea(name, code, "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建合作区应成功");
        String areaId = idOf(addRoot.getAsJsonObject("data"));

        String resp = api.addCooperationAreaUser(areaId, "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "添加合作区用户应成功, resp: " + resp);
        // Cleanup
        api.deleteCooperationAreaUser(areaId, "admin");
        api.deleteCooperationArea(areaId);
        log.info("添加合作区用户 通过: areaId={}, userId=admin", areaId);
    }

    @Test
    @DisplayName("删除合作区用户(正向)")
    void test_deleteCooperationAreaUser() {
        String name = "AT_AreaDelUser_" + suffix();
        String code = "ATCDU" + suffix();
        String addResp = api.addCooperationArea(name, code, "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建合作区应成功");
        String areaId = idOf(addRoot.getAsJsonObject("data"));

        api.addCooperationAreaUser(areaId, "admin");

        String resp = api.deleteCooperationAreaUser(areaId, "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "删除合作区用户应成功, resp: " + resp);
        // Cleanup
        api.deleteCooperationArea(areaId);
        log.info("删除合作区用户 通过: areaId={}, userId=admin", areaId);
    }

    @Test
    @DisplayName("添加合作区用户-空areaId(负向)")
    void test_addCooperationAreaUserEmptyAreaId() {
        String resp = api.addCooperationAreaUser("", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("添加合作区用户-空areaId: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("添加合作区用户-无效areaId(负向)")
    void test_addCooperationAreaUserInvalidAreaId() {
        String resp = api.addCooperationAreaUser("invalid_id_99999", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "无效areaId应被拦截");
        log.info("添加合作区用户-无效areaId 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("添加合作区用户-无效userId(负向)")
    void test_addCooperationAreaUserInvalidUserId() {
        String name = "AT_InvUser_" + suffix();
        String addResp = api.addCooperationArea(name, "ATCIV" + suffix(), "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        String areaId = null;
        if (addRoot.get("code").getAsInt() == 200 && addRoot.has("data")
                && !addRoot.get("data").isJsonNull()) {
            areaId = idOf(addRoot.getAsJsonObject("data"));
            String resp = api.addCooperationAreaUser(areaId, "nonexistent_user_99999");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("添加合作区用户-无效userId: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        }
        if (areaId != null) api.deleteCooperationArea(areaId);
    }

    @Test
    @DisplayName("重复添加同一用户(负向)")
    void test_addDuplicateUser() {
        String name = "AT_DupUser_" + suffix();
        String addResp = api.addCooperationArea(name, "ATCDUP" + suffix(), "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "新建应成功");
        String areaId = idOf(addRoot.getAsJsonObject("data"));

        api.addCooperationAreaUser(areaId, "admin");
        String resp = api.addCooperationAreaUser(areaId, "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("重复添加用户: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        // Cleanup
        api.deleteCooperationAreaUser(areaId, "admin");
        api.deleteCooperationArea(areaId);
    }

    @Test
    @DisplayName("删除合作区用户-空userId(负向)")
    void test_deleteCooperationAreaUserEmptyUserId() {
        String code = "ATCEM" + suffix();
        String addResp = api.addCooperationArea("AT_空用户_" + suffix(), code, "内部", "");
        JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
        if (addRoot.get("code").getAsInt() == 200 && addRoot.has("data")
                && !addRoot.get("data").isJsonNull()) {
            String areaId = idOf(addRoot.getAsJsonObject("data"));
            String resp = api.deleteCooperationAreaUser(areaId, "");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int c = root.get("code").getAsInt();
            log.info("删除合作区用户-空userId: code={}, msg={}",
                    c, root.has("msg") ? root.get("msg").getAsString() : "");
            api.deleteCooperationArea(areaId);
        }
    }

    // ==================== 绕过出口（尝试突破限制） ====================

    @Test
    @DisplayName("尝试修改不存在的合作区用户(负向)")
    void test_deleteUserFromWrongArea() {
        // Create two areas, add user to area1, try to delete from area2
        String resp1 = api.addCooperationArea("AT_Area1_" + suffix(), "ATAR1" + suffix(), "内部", "");
        String resp2 = api.addCooperationArea("AT_Area2_" + suffix(), "ATAR2" + suffix(), "内部", "");
        JsonObject r1 = JsonParser.parseString(resp1).getAsJsonObject();
        JsonObject r2 = JsonParser.parseString(resp2).getAsJsonObject();
        String id1 = null, id2 = null;
        if (r1.get("code").getAsInt() == 200) id1 = idOf(r1.getAsJsonObject("data"));
        if (r2.get("code").getAsInt() == 200) id2 = idOf(r2.getAsJsonObject("data"));

        if (id1 != null && id2 != null) {
            api.addCooperationAreaUser(id1, "admin");
            // Try to delete user from wrong area
            String resp = api.deleteCooperationAreaUser(id2, "admin");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("从错误合作区删除用户: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
            // Cleanup
            api.deleteCooperationAreaUser(id1, "admin");
        }
        if (id1 != null) api.deleteCooperationArea(id1);
        if (id2 != null) api.deleteCooperationArea(id2);
    }

    @Test
    @DisplayName("删除不存在的合作区用户(负向)")
    void test_deleteNonExistentUser() {
        String addResp = api.addCooperationArea("AT_NoUser_" + suffix(), "ATCNU" + suffix(), "内部", "");
        JsonObject root = JsonParser.parseString(addResp).getAsJsonObject();
        if (root.get("code").getAsInt() == 200 && root.has("data")
                && !root.get("data").isJsonNull()) {
            String id = idOf(root.getAsJsonObject("data"));
            String resp = api.deleteCooperationAreaUser(id, "admin");
            JsonObject r = JsonParser.parseString(resp).getAsJsonObject();
            int code = r.get("code").getAsInt();
            log.info("删除不存在用户: code={}, msg={}",
                    code, r.has("msg") ? r.get("msg").getAsString() : "");
            api.deleteCooperationArea(id);
        }
    }

    // ==================== Helper ====================

    private void cleanupCoopAreaByName(String name) {
        String searchResp = api.searchCooperationAreaList(name, "");
        try {
            JsonObject sr = JsonParser.parseString(searchResp).getAsJsonObject();
            if (sr.has("data") && sr.get("data").isJsonArray()) {
                var arr = sr.getAsJsonArray("data");
                for (var el : arr) {
                    JsonObject item = el.getAsJsonObject();
                    if (name.equals(item.get("name").getAsString())) {
                        api.deleteCooperationArea(idOf(item));
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
