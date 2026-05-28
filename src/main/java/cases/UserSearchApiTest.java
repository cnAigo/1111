package cases;

import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserSearchApiTest extends BaseTest {

    @Test
    @DisplayName("搜索用户-存在的用户(正向)")
    void test_searchUserExists() {
        String resp = api.searchUser("admin");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索用户应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        Assertions.assertTrue(root.get("data").isJsonArray(), "data应为数组");
        JsonArray data = root.getAsJsonArray("data");
        Assertions.assertTrue(data.size() > 0, "搜索admin应至少返回1条结果");
        log.info("搜索用户-存在 通过: 找到 {} 条结果", data.size());
    }

    @Test
    @DisplayName("搜索用户-部分关键字(正向)")
    void test_searchUserPartialKeyword() {
        String resp = api.searchUser("ad");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "部分关键字搜索应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("搜索用户-部分关键字 通过: dataSize={}",
                root.getAsJsonArray("data").size());
    }

    @Test
    @DisplayName("搜索用户-不存在的用户(负向)")
    void test_searchUserNotExists() {
        String resp = api.searchUser("NonExistentUser_" + suffix());

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索不存在的用户请求本身应成功");
        Assertions.assertTrue(api.isDataEmpty(resp),
                "不存在的用户应返回空data");
        log.info("搜索用户-不存在 通过: data为空");
    }

    @Test
    @DisplayName("搜索用户-空关键字(负向)")
    void test_searchUserEmptyKeyword() {
        String resp = api.searchUser("");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("搜索用户-空关键字: code={}, dataSize={}",
                root.get("code").getAsInt(),
                root.has("data") && !root.get("data").isJsonNull()
                        ? root.getAsJsonArray("data").size() : "null");
    }

    @Test
    @DisplayName("搜索用户-特殊字符(负向)")
    void test_searchUserSpecialChars() {
        String resp = api.searchUser("'; DROP TABLE users;--");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(500, code,
                "SQL注入类输入不应导致500, resp: " + resp);
        Assertions.assertTrue(api.isDataEmpty(resp) || code == 200,
                "特殊字符输入应安全处理");
        log.info("搜索用户-特殊字符 通过: code={}", code);
    }

    @Test
    @DisplayName("搜索用户-超长关键字(负向)")
    void test_searchUserTooLong() {
        String longKeyword = "A".repeat(200);
        String resp = api.searchUser(longKeyword);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(500, code,
                "超长关键字不应导致500, code=" + code);
        log.info("搜索用户-超长关键字 通过: code={}", code);
    }

    @Test
    @DisplayName("搜索用户-中文字符(正向)")
    void test_searchUserChinese() {
        String resp = api.searchUser("管理员");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "中文搜索应成功, resp: " + resp);
        log.info("搜索用户-中文 通过: dataSize={}",
                root.has("data") && !root.get("data").isJsonNull()
                        ? root.getAsJsonArray("data").size() : "null");
    }
}
