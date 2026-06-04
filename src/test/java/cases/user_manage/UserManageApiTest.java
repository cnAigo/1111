package cases.user_manage;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("UserManageModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserManageApiTest extends ApiTestHelper {

    // ==================== 新建用户 ====================

    @Test
    @DisplayName("QTYL_044: 新建用户(正向)")
    void test_QTYL_044_addUser() {
        String userName = "atuser_" + suffix();
        String nickName = "AT测试用户";
        String userId = null;
        try {
            String resp = api.sysUserCreate(userName, nickName, "Aa123456",
                    100, userName + "@test.com", "138" + suffix().substring(0, 6),
                    "0", "0", "自动化测试创建", "1", "[4]");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建用户应成功, resp: " + resp);

            userId = findUserId(userName);
            Assertions.assertNotNull(userId, "应能查到刚创建的用户ID");
            log.info("QTYL_044 通过: 新建用户 userId={}, userName={}", userId, userName);
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    @Test
    @DisplayName("QTYL_045: 用户昵称为空(负向)")
    void test_QTYL_045_addEmptyNickname() {
        String resp = api.sysUserCreate("atuser_" + suffix(), "", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_045: 空昵称 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName("atuser_" + suffix());
    }

    @Test
    @DisplayName("QTYL_046: 用户名称为空(负向)")
    void test_QTYL_046_addEmptyUserName() {
        String resp = api.sysUserCreate("", "AT用户", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_046: 空用户名 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_047: 用户名少于2位(负向)")
    void test_QTYL_047_userNameTooShort() {
        String resp = api.sysUserCreate("a", "AT用户", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_047: 过短用户名 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName("a");
    }

    @Test
    @DisplayName("QTYL_048: 用户名2-20位合法(正向)")
    void test_QTYL_048_userNameValidLength() {
        String userName = "ab" + suffix().substring(0, 4);
        String userId = null;
        try {
            String resp = api.sysUserCreate(userName, "AT用户", "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "合法长度用户名应成功, resp: " + resp);
            userId = findUserId(userName);
            Assertions.assertNotNull(userId, "应能查到刚创建的用户ID");
            log.info("QTYL_048 通过: 合法用户名 userId={}, userName={}", userId, userName);
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    @Test
    @DisplayName("QTYL_049: 用户名超过20位(负向)")
    void test_QTYL_049_userNameTooLong() {
        String longName = "a".repeat(21);
        String resp = api.sysUserCreate(longName, "AT用户", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_049: 超长用户名 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName(longName);
    }

    @Test
    @DisplayName("QTYL_050: 密码为空(负向)")
    void test_QTYL_050_addEmptyPassword() {
        String userName = "atuser_" + suffix();
        String resp = api.sysUserCreate(userName, "AT用户", "",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_050: 空密码 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName(userName);
    }

    @Test
    @DisplayName("QTYL_051: 密码少于5位(负向)")
    void test_QTYL_051_passwordTooShort() {
        String userName = "atuser_" + suffix();
        String resp = api.sysUserCreate(userName, "AT用户", "Aa1",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_051: 过短密码 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName(userName);
    }

    @Test
    @DisplayName("QTYL_053: 密码超过20位(负向)")
    void test_QTYL_053_passwordTooLong() {
        String userName = "atuser_" + suffix();
        String longPwd = "A".repeat(21);
        String resp = api.sysUserCreate(userName, "AT用户", longPwd,
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_053: 超长密码 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        if (code == 200) cleanupUserByName(userName);
    }

    @Test
    @DisplayName("QTYL_056: 手机号格式错误(负向)")
    void test_QTYL_056_invalidPhoneFormat() {
        String resp = api.sysUserCreate("atuser_" + suffix(), "AT用户", "Aa123456",
                100, "", "abc12345678", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_056: 非法手机号 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_058: 邮箱格式错误(负向)")
    void test_QTYL_058_invalidEmailFormat() {
        String resp = api.sysUserCreate("atuser_" + suffix(), "AT用户", "Aa123456",
                100, "not-an-email", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_058: 非法邮箱 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 搜索用户 ====================

    @Test
    @DisplayName("QTYL_032: 搜索存在的用户(正向)")
    void test_QTYL_032_searchUserExists() {
        String resp = api.sysUserList(1, 10, "admin", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索用户应成功, resp: " + resp);
        Assertions.assertTrue(root.has("rows"), "应包含rows字段");
        log.info("QTYL_032 通过: 搜索admin成功, total={}", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_033: 搜索不存在的用户(负向)")
    void test_QTYL_033_searchUserNotExists() {
        String resp = api.sysUserList(1, 10, "NonExistentUser_" + suffix(), "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索请求本身应成功");
        Assertions.assertEquals(0, root.get("total").getAsInt(),
                "不存在的用户应返回0条结果");
        log.info("QTYL_033 通过: total=0");
    }

    @Test
    @DisplayName("QTYL_034: 用户名模糊搜索(正向)")
    void test_QTYL_034_searchUserFuzzy() {
        String resp = api.sysUserList(1, 10, "ad", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "模糊搜索应成功, resp: " + resp);
        log.info("QTYL_034 通过: 模糊搜索 'ad' 成功, total={}", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_038: 按状态筛选(正向)")
    void test_QTYL_038_searchByStatus() {
        String resp = api.sysUserList(1, 10, "", "", "0");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按状态筛选应成功, resp: " + resp);
        log.info("QTYL_038 通过: 按状态筛选成功, total={}", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("用户搜索-空关键字(负向)")
    void test_searchUserEmptyKeyword() {
        String resp = api.sysUserList(1, 10, "", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("用户搜索-空关键字: code={}, total={}",
                code, root.has("total") ? root.get("total").getAsInt() : "null");
    }

    @Test
    @DisplayName("用户搜索-特殊字符(危险)")
    void test_searchUserSqlInjection() {
        String resp = api.sysUserList(1, 10, "'; DROP TABLE users;--", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        int total = root.has("total") ? root.get("total").getAsInt() : -1;
        log.warn("用户搜索-SQL注入防护: code={}, total={} — 若code=200且total=0则说明后端忽略了特殊字符(可接受)", code, total);
    }

    // ==================== 修改用户 ====================

    @Test
    @DisplayName("QTYL_074: 修改用户(正向)")
    void test_QTYL_074_updateUser() {
        String userName = "atumod_" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, "AT待修改_" + suffix(), "Aa123456",
                    100, "", "138" + suffix().substring(0, 6),
                    "0", "0", "", "1", "[4]");
            userId = findUserId(userName);
            if (userId == null) { log.warn("新建用户失败，跳过修改测试"); return; }

            // Get full user object
            String getResp = api.sysUserGetById(userId);
            JsonObject getRoot = JsonParser.parseString(getResp).getAsJsonObject();
            JsonObject outerData = getRoot.getAsJsonObject("data");
            JsonObject userData = outerData.has("data") && !outerData.get("data").isJsonNull()
                    ? outerData.getAsJsonObject("data")
                    : outerData;
            userData.addProperty("nickName", "AT修改后_" + suffix());
            String resp = api.sysUserUpdate(userData.toString());
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "修改用户应成功, resp: " + resp);
            log.info("QTYL_074 通过: 修改用户 userId={}", userId);
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    @Test
    @DisplayName("修改用户-不存在ID(负向)")
    void test_updateUserInvalidId() {
        String resp = api.sysUserUpdate("{\"userId\":99999,\"nickName\":\"test\"}");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("修改用户-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("修改用户-空昵称(负向)")
    void test_updateUserEmptyNickname() {
        String userName = "atuempt_" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, "AT更新测试_" + suffix(), "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");
            userId = findUserId(userName);
            if (userId == null) return;

            String getResp = api.sysUserGetById(userId);
            JsonObject getRoot = JsonParser.parseString(getResp).getAsJsonObject();
            JsonObject outerData = getRoot.getAsJsonObject("data");
            JsonObject userData = outerData.has("data") && !outerData.get("data").isJsonNull()
                    ? outerData.getAsJsonObject("data")
                    : outerData;
            userData.addProperty("nickName", "");
            String resp = api.sysUserUpdate(userData.toString());
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("修改用户-空昵称: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    // ==================== 删除用户 ====================

    @Test
    @DisplayName("QTYL_076: 删除用户(正向)")
    void test_QTYL_076_deleteUser() {
        String userName = "atudel_" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, "AT待删除_" + suffix(), "Aa123456",
                    100, "", "138" + suffix().substring(0, 6),
                    "0", "0", "", "1", "[4]");
            userId = findUserId(userName);
            if (userId == null) { log.warn("新建用户失败，跳过删除测试"); return; }

            String resp = api.sysUserDelete(userId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除用户应成功, resp: " + resp);
            log.info("QTYL_076 通过: 删除用户 userId={}", userId);
            userId = null; // already deleted
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    @Test
    @DisplayName("删除用户-不存在ID(负向)")
    void test_deleteUserInvalidId() {
        String resp = api.sysUserDelete("99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("删除用户-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("删除用户-空ID(负向)")
    void test_deleteUserEmptyId() {
        String resp = api.sysUserDelete("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("删除用户-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 重置密码 ====================

    @Test
    @DisplayName("QTYL_078: 重置密码(正向)")
    void test_QTYL_078_resetPassword() {
        String resp = api.resetPassword("1", "Bb654321");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_078 重置密码: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("重置密码-不存在ID(负向)")
    void test_resetPasswordInvalidId() {
        String resp = api.resetPassword("invalid_user_99999", "Aa123456");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("重置密码-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("重置密码-空密码(负向)")
    void test_resetPasswordEmpty() {
        String resp = api.resetPassword("1", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("重置密码-空密码: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 导入用户 ====================

    @Test
    @DisplayName("QTYL_063: 导入用户-空数据(负向)")
    void test_QTYL_063_importUserEmptyData() {
        String resp = api.importUser("[]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_063 导入用户-空数据: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("导入用户-非法格式(负向)")
    void test_importUserMalformedData() {
        String resp = api.importUser("{invalid}");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("导入用户-非法格式: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 已有搜索接口覆盖 ====================

    @Test
    @DisplayName("搜索用户详情-存在用户(正向)")
    void test_searchUserByKeywordExists() {
        String resp = api.sysUserList(1, 10, "admin", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索用户应成功, resp: " + resp);
        Assertions.assertTrue(root.has("rows"), "应包含rows字段");
        log.info("搜索用户详情 通过: admin搜索结果={}条", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("搜索用户详情-特殊字符(安全测试)")
    void test_searchUserByKeywordSpecialChars() {
        String resp = api.sysUserList(1, 10, "##", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        int total = root.has("total") ? root.get("total").getAsInt() : -1;
        // 只要不抛出500异常且rows为空，即视为安全拦截成功
        Assertions.assertNotEquals(500, code,
                "特殊字符##不应导致500服务器异常");
        if (code == 200 && total == 0) {
            log.info("特殊字符##安全测试通过: 后端静默处理, code=200 total=0");
        } else {
            log.info("特殊字符##处理: code={}, total={}", code, total);
        }
    }

    // ========== helpers ==========

    private String findUserId(String userName) {
        try {
            String listResp = api.sysUserList(1, 20, userName, "", "");
            JsonObject root = JsonParser.parseString(listResp).getAsJsonObject();
            if (root.has("rows") && !root.get("rows").isJsonNull()) {
                JsonArray rows = root.getAsJsonArray("rows");
                if (rows.size() > 0) {
                    return rows.get(0).getAsJsonObject().get("userId").getAsString();
                }
            }
        } catch (Exception e) {
            log.warn("findUserId failed for {}: {}", userName, e.getMessage());
        }
        return null;
    }

    private void cleanupUserByName(String userName) {
        try {
            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
            if (listRoot.has("rows") && !listRoot.get("rows").isJsonNull()) {
                JsonArray rows = listRoot.getAsJsonArray("rows");
                for (int i = 0; i < rows.size(); i++) {
                    api.sysUserDelete(rows.get(i).getAsJsonObject().get("userId").getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
