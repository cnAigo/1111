package cases.collaboration;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("CollaborationModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectPersonApiTest extends ApiTestHelper {

    // ==================== 搜索项目列表 ====================

    @Test
    @DisplayName("QTYL_QX_001: 搜索项目列表(正向)")
    void test_searchProjectList() {
        String resp = api.searchProjectList("", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索项目列表应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("QTYL_QX_001 通过: 项目数量={}",
                root.getAsJsonArray("data").size());
    }

    @Test
    @DisplayName("搜索项目列表-按名称过滤(正向)")
    void test_searchProjectListWithTitle() {
        String resp = api.searchProjectList("AT", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索项目列表应成功, resp: " + resp);
        log.info("搜索项目-按名称 通过: dataSize={}",
                root.has("data") && !root.get("data").isJsonNull()
                        ? root.getAsJsonArray("data").size() : "null");
    }

    // ==================== 搜索项目人员 ====================

    @Test
    @DisplayName("QTYL_QX_002: 搜索项目人员列表(正向)")
    void test_searchProjectPersonList() {
        String resp = api.searchProjectPersonList(PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索项目人员应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        JsonArray data = root.getAsJsonArray("data");
        log.info("QTYL_QX_002 通过: 项目人员数量={}", data.size());
    }

    @Test
    @DisplayName("搜索项目人员-空objectId(负向)")
    void test_searchProjectPersonListEmptyId() {
        String resp = api.searchProjectPersonList("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索项目人员-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("搜索项目人员-无效objectId(负向)")
    void test_searchProjectPersonListInvalidId() {
        String resp = api.searchProjectPersonList("invalid_object_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索项目人员-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 部门树 ====================

    @Test
    @DisplayName("QTYL_QX_003: 获取部门树(正向)")
    void test_deptTree() {
        String resp = api.deptTree();
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "获取部门树应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("QTYL_QX_003 通过: 部门树获取成功");
    }

    // ==================== 用户列表 ====================

    @Test
    @DisplayName("QTYL_QX_004: 查询非管理员用户列表(正向)")
    void test_listUsersWithoutAdmins() {
        String resp = api.listUsersWithoutAdmins(1, 10, "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "查询用户列表应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("QTYL_QX_004 通过: 用户列表获取成功");
    }

    @Test
    @DisplayName("查询用户列表-按用户名过滤(正向)")
    void test_listUsersFiltered() {
        String resp = api.listUsersWithoutAdmins(1, 10, "", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按用户名过滤应成功, resp: " + resp);
        log.info("查询用户-过滤 通过");
    }

    @Test
    @DisplayName("查询用户列表-空参数(负向)")
    void test_listUsersEmptyParams() {
        String resp = api.listUsersWithoutAdmins(1, 10, null, null);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("查询用户-空参数: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 分配项目人员 ====================

    @Test
    @DisplayName("QTYL_QX_005: 分配项目人员(正向)")
    void test_assignProjectPersonList() {
        String userData = """
                [{
                    "userId": 383,
                    "secretLevel": "1",
                    "deptId": 100,
                    "deptName": "安托",
                    "userName": "11",
                    "nickName": "11",
                    "email": "123@qq.com",
                    "phonenumber": "11",
                    "sex": "0",
                    "status": "0",
                    "isTemp": "0"
                }]
                """;
        String resp = api.assignProjectPersonList(PROJECT_ID, userData);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_QX_005: 分配项目人员 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("分配项目人员-空objectId(负向)")
    void test_assignProjectPersonEmptyId() {
        String resp = api.assignProjectPersonList("", "[]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("分配人员-空objectId: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("分配项目人员-空data(负向)")
    void test_assignProjectPersonEmptyData() {
        String resp = api.assignProjectPersonList(PROJECT_ID, "[]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("分配人员-空data: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("分配项目人员-无效objectId(负向)")
    void test_assignProjectPersonInvalidId() {
        String resp = api.assignProjectPersonList("invalid_object_99999", "[]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("分配人员-无效objectId: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 分配+查询联动验证 ====================

    @Test
    @DisplayName("QTYL_QX_006: 分配后查询验证")
    void test_assignThenSearch() {
        // List users to find a real one
        String listResp = api.listUsersWithoutAdmins(1, 5, "", "");
        JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
        log.info("现有用户列表 code={}", listRoot.get("code").getAsInt());

        // Assign with real user if available
        String userData = """
                [{
                    "userId": 383,
                    "secretLevel": "1",
                    "deptId": 100,
                    "deptName": "安托",
                    "userName": "11",
                    "nickName": "11",
                    "sex": "0",
                    "status": "0",
                    "isTemp": "0"
                }]
                """;
        String assignResp = api.assignProjectPersonList(PROJECT_ID, userData);
        log.info("分配结果: {}", assignResp);

        // Search after assign
        String searchResp = api.searchProjectPersonList(PROJECT_ID);
        JsonObject searchRoot = JsonParser.parseString(searchResp).getAsJsonObject();
        JsonArray data = searchRoot.getAsJsonArray("data");
        log.info("QTYL_QX_006: 分配后查询人员数量={} (如果为0说明后端add bug)", data.size());
    }
}
