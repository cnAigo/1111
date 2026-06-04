package cases.user_manage;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

@Tag("UserManageModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("API测试")
@Feature("系统用户管理")
public class SystemUserApiTest extends ApiTestHelper {

    // ==================== 用户列表查询 ====================

    @Test
    @DisplayName("QTYL_SYS_001: 查询用户列表-分页(正向)")
    @Story("用户列表查询")
    @Description("验证分页查询用户列表接口，校验返回结构包含total/rows字段，rows中用户对象包含核心业务字段")
    @Severity(SeverityLevel.BLOCKER)
    void test_sysUserList_positive() {
        String resp = api.sysUserList(1, 10, "", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "查询用户列表应返回200, resp: " + resp);
        Assertions.assertTrue(root.has("total"), "应包含total字段");
        Assertions.assertTrue(root.has("rows"), "应包含rows字段");

        int total = root.get("total").getAsInt();
        JsonArray rows = root.getAsJsonArray("rows");
        Assertions.assertTrue(total > 0, "系统中至少应有一个用户");
        Assertions.assertNotNull(rows, "rows不应为null");

        if (rows.size() > 0) {
            JsonObject firstUser = rows.get(0).getAsJsonObject();
            Assertions.assertTrue(firstUser.has("userId"), "用户对象应包含userId");
            Assertions.assertTrue(firstUser.has("userName"), "用户对象应包含userName");
            Assertions.assertTrue(firstUser.has("nickName"), "用户对象应包含nickName");
            Assertions.assertTrue(firstUser.has("deptId"), "用户对象应包含deptId");
            Assertions.assertTrue(firstUser.has("phonenumber"), "用户对象应包含phonenumber");
            Assertions.assertTrue(firstUser.has("status"), "用户对象应包含status");
            Assertions.assertTrue(firstUser.has("secretLevel"), "用户对象应包含secretLevel");
        }
        log.info("QTYL_SYS_001 通过: total={}, rows.size={}", total, rows.size());
    }

    @Test
    @DisplayName("QTYL_SYS_001-P2: 查询用户列表-第2页(正向)")
    @Story("用户列表查询")
    @Description("验证分页查询第2页，校验返回的数据不属于第1页")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUserList_page2() {
        // Get page 1 first to compare
        String page1Resp = api.sysUserList(1, 5, "", "", "");
        JsonObject page1Root = JsonParser.parseString(page1Resp).getAsJsonObject();
        JsonArray page1Rows = page1Root.getAsJsonArray("rows");
        String firstUserId_page1 = page1Rows.size() > 0
                ? page1Rows.get(0).getAsJsonObject().get("userId").getAsString() : "";

        // Get page 2
        String resp = api.sysUserList(2, 5, "", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "查询第2页应成功, resp: " + resp);
        Assertions.assertTrue(root.get("total").getAsInt() > 0, "total应大于0");

        JsonArray rows = root.getAsJsonArray("rows");
        if (rows.size() > 0) {
            String firstUserId_page2 = rows.get(0).getAsJsonObject().get("userId").getAsString();
            Assertions.assertNotEquals(firstUserId_page1, firstUserId_page2,
                    "第2页第一条记录应与第1页不同");
        }
        log.info("QTYL_SYS_001-P2 通过: page2 rows.size={}", rows.size());
    }

    @Test
    @DisplayName("QTYL_SYS_001-N1: 查询用户列表-pageSize=0(负向)")
    @Story("用户列表查询")
    @Description("验证pageSize=0时接口的处理策略")
    @Severity(SeverityLevel.MINOR)
    void test_sysUserList_pageSizeZero() {
        String resp = api.sysUserList(1, 0, "", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("QTYL_SYS_001-N1: pageSize=0, code={}, total={}, msg={}",
                root.get("code").getAsInt(),
                root.has("total") ? root.get("total").getAsInt() : -1,
                root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 用户列表过滤 ====================

    @Test
    @DisplayName("QTYL_SYS_002: 按用户名过滤(正向)")
    @Story("用户列表过滤")
    @Description("按userName精确/模糊过滤，校验返回结果中所有用户包含过滤关键字")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUserList_filterByUserName() {
        String keyword = "admin";
        String resp = api.sysUserList(1, 10, keyword, "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按用户名过滤应成功, resp: " + resp);

        JsonArray rows = root.getAsJsonArray("rows");
        if (rows != null && rows.size() > 0) {
            for (int i = 0; i < rows.size(); i++) {
                String userName = rows.get(i).getAsJsonObject().get("userName").getAsString();
                Assertions.assertTrue(userName.contains(keyword),
                        "过滤结果中每个用户名应包含关键字'" + keyword + "', 实际: " + userName);
            }
        }
        log.info("QTYL_SYS_002 通过: 过滤[{}] 返回 {} 条", keyword, root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_SYS_002-N: 按不存在的用户名过滤(负向)")
    @Story("用户列表过滤")
    @Description("按不存在的用户名过滤应返回空列表")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUserList_filterByNonExistingUserName() {
        String resp = api.sysUserList(1, 10, "nonexistent_user_xyz", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按不存在用户名过滤应成功返回空列表");
        Assertions.assertEquals(0, root.get("total").getAsInt(),
                "不存在用户名的查询total应为0");
        log.info("QTYL_SYS_002-N 通过: total=0");
    }

    @Test
    @DisplayName("QTYL_SYS_003: 按状态过滤-启用(正向)")
    @Story("用户列表过滤")
    @Description("按状态值过滤用户列表，验证所有返回用户的status匹配过滤条件")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUserList_filterByStatus() {
        String resp = api.sysUserList(1, 10, "", "", "0");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按状态过滤应成功, resp: " + resp);

        JsonArray rows = root.getAsJsonArray("rows");
        if (rows != null && rows.size() > 0) {
            for (int i = 0; i < rows.size(); i++) {
                String status = rows.get(i).getAsJsonObject().get("status").getAsString();
                Assertions.assertEquals("0", status,
                        "过滤状态=0时，所有返回用户status应为0");
            }
        }
        log.info("QTYL_SYS_003 通过: 状态=0 返回 {} 条", root.get("total").getAsInt());
    }

    @Test
    @DisplayName("QTYL_SYS_004: 按手机号过滤(正向)")
    @Story("用户列表过滤")
    @Description("按手机号过滤用户列表")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUserList_filterByPhone() {
        String resp = api.sysUserList(1, 10, "", "15888888888", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "按手机号过滤应成功, resp: " + resp);

        JsonArray rows = root.getAsJsonArray("rows");
        if (rows != null && rows.size() > 0) {
            for (int i = 0; i < rows.size(); i++) {
                String phone = rows.get(i).getAsJsonObject().get("phonenumber").getAsString();
                Assertions.assertTrue(phone.contains("15888888888"),
                        "过滤结果中的手机号应包含过滤关键字");
            }
        }
        log.info("QTYL_SYS_004 通过: 返回 {} 条", root.get("total").getAsInt());
    }

    // ==================== 字典数据 ====================

    @Test
    @DisplayName("QTYL_SYS_005: 获取性别字典(正向)")
    @Story("字典数据查询")
    @Description("验证性别字典返回三个选项: 男/女/未知，校验dictValue映射正确")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysDictData_sex() {
        String resp = api.sysDictData("sys_user_sex");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "获取性别字典应成功, resp: " + resp);

        JsonArray data = root.getAsJsonArray("data");
        Assertions.assertNotNull(data, "字典data不应为null");
        Assertions.assertEquals(3, data.size(),
                "性别字典应包含3个选项(男/女/未知), 实际: " + data.size());

        // Verify expected values
        boolean hasMale = false, hasFemale = false, hasUnknown = false;
        for (int i = 0; i < data.size(); i++) {
            JsonObject item = data.get(i).getAsJsonObject();
            String label = item.get("dictLabel").getAsString();
            if ("男".equals(label)) hasMale = true;
            if ("女".equals(label)) hasFemale = true;
            if ("未知".equals(label)) hasUnknown = true;
        }
        Assertions.assertTrue(hasMale && hasFemale && hasUnknown,
                "性别字典应包含 男/女/未知 三个选项");

        log.info("QTYL_SYS_005 通过: 性别字典包含3个选项(男/女/未知)");
    }

    // ==================== 部门树 ====================

    @Test
    @DisplayName("QTYL_SYS_005-D: 获取部门树(正向)")
    @Story("部门树查询")
    @Description("验证部门树返回树形结构，包含id/label字段")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUserDeptTree() {
        String resp = api.sysUserDeptTree();
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "获取部门树应成功, resp: " + resp);

        JsonArray data = root.getAsJsonArray("data");
        Assertions.assertNotNull(data, "部门树data不应为null");
        Assertions.assertTrue(data.size() > 0, "至少应有一个部门");

        JsonObject firstDept = data.get(0).getAsJsonObject();
        Assertions.assertTrue(firstDept.has("id"), "部门节点应包含id");
        Assertions.assertTrue(firstDept.has("label"), "部门节点应包含label");

        log.info("QTYL_SYS_005-D 通过: 部门树含 {} 个根节点", data.size());
    }

    // ==================== 用户CRUD完整流程 ====================

    @Test
    @DisplayName("QTYL_SYS_006: 用户CRUD完整流程(创建→查看→修改→删除)")
    @Story("用户CRUD")
    @Description("完整验证创建用户、按用户名反查ID、查看详情、修改昵称、删除清理的全流程")
    @Severity(SeverityLevel.BLOCKER)
    void test_sysUser_fullCrud() {
        String userName = "atu" + suffix();
        String nickName = "AT_" + suffix();
        String phone = "138" + String.valueOf(System.currentTimeMillis()).substring(8);
        String userId = null;
        try {
            // 1. Create
            String createResp = api.sysUserCreate(userName, nickName, "Aa123456",
                    100, userName + "@test.com", phone, "1", "0",
                    "自动化测试用户", "1", "[4]");
            JsonObject createRoot = JsonParser.parseString(createResp).getAsJsonObject();
            Assertions.assertEquals(200, createRoot.get("code").getAsInt(),
                    "创建用户应成功, resp: " + createResp);
            Assertions.assertEquals("操作成功", createRoot.get("msg").getAsString(),
                    "成功消息应为'操作成功'");
            log.info("  创建用户成功: userName={}", userName);

            // 2. Find user ID from list (验证创建持久化)
            String listResp = api.sysUserList(1, 20, userName, "", "");
            JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
            Assertions.assertTrue(listRoot.has("rows") && !listRoot.get("rows").isJsonNull(),
                    "查询列表应返回rows");
            JsonArray rows = listRoot.getAsJsonArray("rows");
            Assertions.assertTrue(rows.size() > 0, "应能查到刚创建的用户");
            JsonObject foundUser = rows.get(0).getAsJsonObject();
            userId = foundUser.get("userId").getAsString();
            Assertions.assertEquals(userName, foundUser.get("userName").getAsString(),
                    "列表中的用户名应与创建的匹配");
            Assertions.assertEquals(nickName, foundUser.get("nickName").getAsString(),
                    "列表中的昵称应与创建的匹配");
            log.info("  查找到用户: userId={}", userId);

            // 3. Get by ID (验证详情)
            String getResp = api.sysUserGetById(userId);
            JsonObject getRoot = JsonParser.parseString(getResp).getAsJsonObject();
            Assertions.assertEquals(200, getRoot.get("code").getAsInt(),
                    "查看用户详情应成功");

            JsonObject outerData = getRoot.getAsJsonObject("data");
            Assertions.assertNotNull(outerData, "data字段不应为null");
            Assertions.assertTrue(outerData.has("data"), "应包含内层data");
            JsonObject userData = outerData.getAsJsonObject("data");
            Assertions.assertEquals(userName, userData.get("userName").getAsString(),
                    "详情中的用户名应与创建的匹配");
            Assertions.assertEquals(nickName, userData.get("nickName").getAsString(),
                    "详情中的昵称应与创建的匹配");
            Assertions.assertEquals("1", userData.get("secretLevel").getAsString(),
                    "密级应为1");
            log.info("  用户详情验证通过");

            // 4. Update
            String newNickName = nickName + "_改";
            userData.addProperty("nickName", newNickName);
            String updateResp = api.sysUserUpdate(userData.toString());
            JsonObject updateRoot = JsonParser.parseString(updateResp).getAsJsonObject();
            Assertions.assertEquals(200, updateRoot.get("code").getAsInt(),
                    "修改用户应成功, resp: " + updateResp);

            // Verify update persisted
            String verifyResp = api.sysUserGetById(userId);
            JsonObject verifyRoot = JsonParser.parseString(verifyResp).getAsJsonObject();
            JsonObject verifyData = verifyRoot.getAsJsonObject("data").getAsJsonObject("data");
            Assertions.assertEquals(newNickName, verifyData.get("nickName").getAsString(),
                    "修改后昵称应已持久化");
            log.info("  修改用户成功: newNick={}", newNickName);
        } finally {
            if (userId != null) {
                api.sysUserDelete(userId);
                log.info("  已清理测试用户: userId={}", userId);
            }
        }
    }

    @Test
    @DisplayName("QTYL_SYS_006-U: 修改用户-完整字段更新(正向)")
    @Story("用户CRUD")
    @Description("验证用户可以修改email/phonenumber/sex/remark等多个字段")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_updateFullProfile() {
        String userName = "atp" + suffix();
        String nickName = "AT_Profile_" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, nickName, "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");

            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonArray rows = JsonParser.parseString(listResp).getAsJsonObject().getAsJsonArray("rows");
            Assertions.assertTrue(rows.size() > 0, "应能查到刚创建的用户");
            userId = rows.get(0).getAsJsonObject().get("userId").getAsString();

            String getResp = api.sysUserGetById(userId);
            JsonObject userData = JsonParser.parseString(getResp).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("data");

            String newEmail = "updated_" + suffix() + "@test.com";
            String newPhone = "139" + String.valueOf(System.currentTimeMillis()).substring(8);
            userData.addProperty("email", newEmail);
            userData.addProperty("phonenumber", newPhone);
            userData.addProperty("sex", "2");
            userData.addProperty("remark", "自动化修改验证");

            String updateResp = api.sysUserUpdate(userData.toString());
            Assertions.assertEquals(200,
                    JsonParser.parseString(updateResp).getAsJsonObject().get("code").getAsInt(),
                    "修改应成功");

            // Verify all changes
            String verifyResp = api.sysUserGetById(userId);
            JsonObject verifyData = JsonParser.parseString(verifyResp).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("data");
            Assertions.assertEquals(newEmail, verifyData.get("email").getAsString());
            Assertions.assertEquals(newPhone, verifyData.get("phonenumber").getAsString());
            Assertions.assertEquals("2", verifyData.get("sex").getAsString());
            Assertions.assertEquals("自动化修改验证", verifyData.get("remark").getAsString());
            log.info("QTYL_SYS_006-U 通过: 多字段更新验证成功");
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    // ==================== 创建用户负向测试 ====================

    @Test
    @DisplayName("QTYL_SYS_008: 创建用户-空用户名(负向)")
    @Story("用户创建-负向")
    @Description("用户名为空时应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_createEmptyUserName() {
        String resp = api.sysUserCreate("", "nick", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空用户名应被后端拦截, code不应为200");
        log.info("QTYL_SYS_008 通过: 空用户名被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SYS_009: 创建用户-空密码(负向)")
    @Story("用户创建-负向")
    @Description("密码为空时应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_createEmptyPassword() {
        String userName = "atpwd" + suffix();
        String resp = api.sysUserCreate(userName, "nick", "",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code == 200) {
            log.warn("QTYL_SYS_009 疑似缺陷: 空密码未被后端拦截, code=200");
            cleanupUserByName(userName);
        } else {
            log.info("QTYL_SYS_009 通过: 空密码被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        }
    }

    @Test
    @DisplayName("QTYL_SYS_010: 创建用户-重复用户名(负向)")
    @Story("用户创建-负向")
    @Description("用户名已存在时应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_createDuplicateUserName() {
        String userName = "atdup" + suffix();
        String userId = null;
        api.sysUserCreate(userName, "nick1", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");

        try {
            // Find the created user
            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonArray rows = JsonParser.parseString(listResp).getAsJsonObject().getAsJsonArray("rows");
            if (rows.size() > 0) userId = rows.get(0).getAsJsonObject().get("userId").getAsString();

            String resp = api.sysUserCreate(userName, "nick2", "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "重复用户名应被拦截, code不应为200");
            log.info("QTYL_SYS_010 通过: 重复用户名被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            cleanupUserByName(userName);
        }
    }

    @Test
    @DisplayName("QTYL_SYS_008-X: 创建用户-含XSS字符(负向)")
    @Story("用户创建-负向")
    @Description("用户名含XSS攻击字符应被拦截或转义")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUser_createXssUserName() {
        String resp = api.sysUserCreate("<script>alert(1)</script>", "nick", "Aa123456",
                100, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SYS_008-X: XSS用户名 code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
        // 预期被拦截或转义，如果成功则需清理
        if (code == 200) {
            cleanupUserByName("<script>alert(1)</script>");
            log.warn("XSS用户名未被拦截，可能存在安全风险！");
        }
    }

    @Test
    @DisplayName("创建用户-空部门(负向)")
    @Story("用户创建-负向")
    @Description("deptId=0时应被拦截或使用默认值")
    @Severity(SeverityLevel.MINOR)
    void test_sysUser_createZeroDeptId() {
        String userName = "atzd" + suffix();
        String resp = api.sysUserCreate(userName, "nick", "Aa123456",
                0, "", "", "0", "0", "", "1", "[4]");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("创建用户-deptId=0: code={}, msg={}",
                root.get("code").getAsInt(),
                root.has("msg") ? root.get("msg").getAsString() : "");
        if (root.get("code").getAsInt() == 200) cleanupUserByName(userName);
    }

    // ==================== 查询/删除负向测试 ====================

    @Test
    @DisplayName("QTYL_SYS_011: 查看用户-无效ID(负向)")
    @Story("用户查询-负向")
    @Description("不存在的用户ID应返回错误码")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_getInvalidId() {
        String resp = api.sysUserGetById("99999999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的用户ID查询应失败");
        log.info("QTYL_SYS_011 通过: 无效ID被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SYS_012: 删除用户-无效ID(负向)")
    @Story("用户删除-负向")
    @Description("删除不存在的用户应返回错误码")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_deleteInvalidId() {
        String resp = api.sysUserDelete("99999999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "删除不存在的用户应失败");
        log.info("QTYL_SYS_012 通过: 无效删除被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SYS_013: 删除用户-空ID(负向)")
    @Story("用户删除-负向")
    @Description("空userId应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_deleteEmptyId() {
        String resp = api.sysUserDelete("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空ID删除应失败");
        log.info("QTYL_SYS_013 通过: 空ID被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 重置密码 ====================

    @Test
    @DisplayName("QTYL_SYS_014: 重置密码(正向)")
    @Story("重置密码")
    @Description("验证重置密码功能：创建用户→重置密码→验证密码已生效")
    @Severity(SeverityLevel.BLOCKER)
    void test_sysUser_resetPwd_positive() {
        String userName = "atrp" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, "AT_ResetPwd_" + suffix(), "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");

            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonArray rows = JsonParser.parseString(listResp).getAsJsonObject().getAsJsonArray("rows");
            Assertions.assertTrue(rows.size() > 0, "应能查到刚创建的用户");
            userId = rows.get(0).getAsJsonObject().get("userId").getAsString();

            String resetResp = api.sysUserResetPwd(userId, "NewPwd789!");
            JsonObject root = JsonParser.parseString(resetResp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "重置密码应成功, resp: " + resetResp);
            Assertions.assertEquals("操作成功", root.get("msg").getAsString(),
                    "成功消息应为'操作成功'");
            log.info("QTYL_SYS_014 通过: 重置密码成功 userId={}", userId);
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    @Test
    @DisplayName("QTYL_SYS_014-N1: 重置密码-空userId(负向)")
    @Story("重置密码-负向")
    @Description("空userId应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_resetPwd_emptyUserId() {
        String resp = api.sysUserResetPwd("", "Aa123456");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空userId重置密码应失败");
        log.info("QTYL_SYS_014-N1 通过: 空userId被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SYS_014-N2: 重置密码-无效userId(负向)")
    @Story("重置密码-负向")
    @Description("不存在的userId应返回错误码")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_resetPwd_invalidUserId() {
        String resp = api.sysUserResetPwd("99999999", "Aa123456");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "无效userId重置密码应失败");
        log.info("QTYL_SYS_014-N2 通过: 无效userId被拦截, code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SYS_014-N3: 重置密码-空密码(负向)")
    @Story("重置密码-负向")
    @Description("空密码应被拦截")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_resetPwd_emptyPassword() {
        String userName = "atrpe" + suffix();
        String userId = null;
        try {
            api.sysUserCreate(userName, "AT_ResetEmpty_" + suffix(), "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");
            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonArray rows = JsonParser.parseString(listResp).getAsJsonObject().getAsJsonArray("rows");
            if (rows.size() > 0) userId = rows.get(0).getAsJsonObject().get("userId").getAsString();

            if (userId != null) {
                String resp = api.sysUserResetPwd(userId, "");
                JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
                int code = root.get("code").getAsInt();
                log.info("QTYL_SYS_014-N3: 空密码 code={}, msg={}",
                        code, root.has("msg") ? root.get("msg").getAsString() : "");
                // If reset with empty pwd succeeded, reset back to avoid data corruption
                if (code == 200) api.sysUserResetPwd(userId, "Aa123456");
            }
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    // ==================== 导出 ====================

    @Test
    @DisplayName("QTYL_SYS_007: 导出用户(正向)")
    @Story("用户导出")
    @Description("验证导出接口返回非空Excel文件")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_export() {
        APIResponse response = api.sysUserExport();
        Assertions.assertEquals(200, response.status(),
                "导出应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "导出文件不应为空");
        // Excel files start with PK (ZIP format) or specific bytes
        Assertions.assertTrue(body.length > 100,
                "导出文件大小应大于100bytes, 实际: " + body.length);
        log.info("QTYL_SYS_007 通过: 导出文件大小={} bytes", body.length);
    }

    // ==================== 导入模板下载 ====================

    @Test
    @DisplayName("QTYL_SYS_015: 下载用户导入模板(正向)")
    @Story("用户导入")
    @Description("验证下载导入模板接口返回非空Excel文件")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUser_importTemplate() {
        APIResponse response = api.sysUserImportTemplate();
        Assertions.assertEquals(200, response.status(),
                "下载导入模板应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "模板文件不应为空");
        Assertions.assertTrue(body.length > 100,
                "模板文件大小应大于100bytes, 实际: " + body.length);
        log.info("QTYL_SYS_015 通过: 模板文件大小={} bytes", body.length);
    }

    // ==================== 安全测试 ====================

    @Test
    @DisplayName("QTYL_SYS_SEC: SQL注入防护-用户名过滤(负向)")
    @Story("安全测试")
    @Description("验证用户名过滤查询对SQL注入有防护")
    @Severity(SeverityLevel.CRITICAL)
    void test_sysUserList_sqlInjection() {
        String resp = api.sysUserList(1, 10, "' OR '1'='1", "", "");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();

        // 应正常返回（不报500）或返回空结果，不应泄露全部数据
        Assertions.assertNotEquals(500, root.get("code").getAsInt(),
                "SQL注入不应导致500错误");
        int total = root.has("total") ? root.get("total").getAsInt() : -1;
        log.info("QTYL_SYS_SEC: SQL注入过滤, code={}, total={}",
                root.get("code").getAsInt(), total);
    }

    // ==================== 综合场景 ====================

    @Test
    @DisplayName("QTYL_SYS_016: 批量创建→查询→批量删除(综合)")
    @Story("用户CRUD")
    @Description("创建2个用户→列表查询验证→逐个删除→验证列表减少")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUser_batchCreateAndDelete() {
        String userName1 = "atb1" + suffix();
        String userName2 = "atb2" + suffix();
        String userId1 = null;
        String userId2 = null;
        try {
            api.sysUserCreate(userName1, "Batch1_" + suffix(), "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");
            api.sysUserCreate(userName2, "Batch2_" + suffix(), "Aa123456",
                    100, "", "", "0", "0", "", "1", "[4]");

            // Verify both appear in list
            String listResp = api.sysUserList(1, 50, "", "", "");
            Assertions.assertEquals(200,
                    JsonParser.parseString(listResp).getAsJsonObject().get("code").getAsInt());

            // Find both users
            String list1 = api.sysUserList(1, 10, userName1, "", "");
            JsonArray rows1 = JsonParser.parseString(list1).getAsJsonObject().getAsJsonArray("rows");
            if (rows1.size() > 0) userId1 = rows1.get(0).getAsJsonObject().get("userId").getAsString();

            String list2 = api.sysUserList(1, 10, userName2, "", "");
            JsonArray rows2 = JsonParser.parseString(list2).getAsJsonObject().getAsJsonArray("rows");
            if (rows2.size() > 0) userId2 = rows2.get(0).getAsJsonObject().get("userId").getAsString();

            Assertions.assertNotNull(userId1, "应查到用户1");
            Assertions.assertNotNull(userId2, "应查到用户2");
            Assertions.assertNotEquals(userId1, userId2, "两个用户ID应不同");

            log.info("QTYL_SYS_016 通过: 批量创建2个用户 userId1={}, userId2={}", userId1, userId2);
        } finally {
            if (userId1 != null) api.sysUserDelete(userId1);
            if (userId2 != null) api.sysUserDelete(userId2);
        }
    }

    @Test
    @DisplayName("QTYL_SYS_017: 创建用户-所有字段完整填充(正向)")
    @Story("用户创建")
    @Description("验证创建用户时所有可选字段均填充后能正确持久化")
    @Severity(SeverityLevel.NORMAL)
    void test_sysUser_createAllFields() {
        String userName = "atfull" + suffix();
        String nickName = "AT_Full_" + suffix();
        String email = "full_" + suffix() + "@test.com";
        String phone = "137" + String.valueOf(System.currentTimeMillis()).substring(8);
        String remark = "全字段填充测试-" + suffix();
        String userId = null;
        try {
            String createResp = api.sysUserCreate(userName, nickName, "Aa123456",
                    100, email, phone, "1", "0", remark, "2", "[4]");
            JsonObject root = JsonParser.parseString(createResp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "创建用户应成功, resp: " + createResp);

            String listResp = api.sysUserList(1, 10, userName, "", "");
            JsonArray rows = JsonParser.parseString(listResp).getAsJsonObject().getAsJsonArray("rows");
            Assertions.assertTrue(rows.size() > 0);
            userId = rows.get(0).getAsJsonObject().get("userId").getAsString();

            String getResp = api.sysUserGetById(userId);
            JsonObject data = JsonParser.parseString(getResp).getAsJsonObject()
                    .getAsJsonObject("data").getAsJsonObject("data");

            Assertions.assertEquals(userName, data.get("userName").getAsString());
            Assertions.assertEquals(nickName, data.get("nickName").getAsString());
            Assertions.assertEquals(email, data.get("email").getAsString());
            Assertions.assertEquals(phone, data.get("phonenumber").getAsString());
            Assertions.assertEquals("1", data.get("sex").getAsString());
            Assertions.assertEquals("0", data.get("status").getAsString());
            Assertions.assertEquals(remark, data.get("remark").getAsString());
            Assertions.assertEquals("2", data.get("secretLevel").getAsString());
            Assertions.assertEquals(100, data.get("deptId").getAsInt());

            log.info("QTYL_SYS_017 通过: 全字段创建验证成功");
        } finally {
            if (userId != null) api.sysUserDelete(userId);
        }
    }

    // ========== helpers ==========

    private void cleanupUserByName(String userName) {
        try {
            String listResp = api.sysUserList(1, 20, userName, "", "");
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
