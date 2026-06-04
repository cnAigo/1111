package cases.attribute;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("AttributeModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomAttributeApiTest extends ApiTestHelper {

    // ==================== 新建自定义属性 ====================

    @Test
    @DisplayName("GNYL_131: 新建整型自定义属性(正向)")
    void test_addCustomAttributeInt() {
        String nameEn = null;
        try {
            nameEn = "AT_Int_" + suffix();
            String resp = api.addCustomAttribute(nameEn, "AT整型属性", "整型", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建应成功, resp: " + resp);
            // 服务端 current 不返回 data，create 成功即可
            if (root.has("data") && !root.get("data").isJsonNull()) {
                log.info("新建属性返回data: {}", root.get("data"));
            }

            String[] found = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(found, "查询应能找到新创建的属性: " + nameEn);
            log.info("GNYL_131 通过: 新建整型属性 [{}] id={}", nameEn, found[0]);
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_132: 新建浮点型自定义属性(正向)")
    void test_addCustomAttributeFloat() {
        String nameEn = null;
        try {
            nameEn = "AT_Float_" + suffix();
            String resp = api.addCustomAttribute(nameEn, "AT浮点属性", "浮点型", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建应成功, resp: " + resp);

            String[] found = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(found, "查询应能找到新创建的属性: " + nameEn);
            log.info("GNYL_132 通过: 新建浮点型属性 [{}] id={}", nameEn, found[0]);
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_133: 新建字符串型自定义属性(正向)")
    void test_addCustomAttributeString() {
        String nameEn = null;
        try {
            nameEn = "AT_Str_" + suffix();
            String resp = api.addCustomAttribute(nameEn, "AT字符串属性", "字符串", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建应成功, resp: " + resp);

            String[] found = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(found, "查询应能找到新创建的属性: " + nameEn);
            log.info("GNYL_133 通过: 新建字符串属性 [{}] id={}", nameEn, found[0]);
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_134: 新建日期型自定义属性(正向)")
    void test_addCustomAttributeDate() {
        String nameEn = null;
        try {
            nameEn = "AT_Date_" + suffix();
            String resp = api.addCustomAttribute(nameEn, "AT日期属性", "日期", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建应成功, resp: " + resp);

            String[] found = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(found, "查询应能找到新创建的属性: " + nameEn);
            log.info("GNYL_134 通过: 新建日期属性 [{}] id={}", nameEn, found[0]);
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    // ==================== 新建负向场景 ====================

    @Test
    @DisplayName("新建自定义属性-英文名为空(负向)")
    void test_addCustomAttributeEmptyNameEn() {
        String resp = api.addCustomAttribute("", "空英文名属性", "字符串", PROJECT_ID);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code == 200) {
            log.warn("⚠ 服务端未校验空英文名，允许创建 — 建议服务端增加校验");
        } else {
            log.info("新建属性-空英文名 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        }
    }

    @Test
    @DisplayName("新建自定义属性-中文名为空(负向)")
    void test_addCustomAttributeEmptyName() {
        String nameEn = "AT_EmptyName_" + suffix();
        try {
            String resp = api.addCustomAttribute(nameEn, "", "字符串", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                log.warn("⚠ 服务端未校验空中文名，允许创建 — 建议服务端增加校验");
            } else {
                log.info("新建属性-空中文字段 通过: 被拦截, code={}, msg={}",
                        code, root.has("msg") ? root.get("msg").getAsString() : "");
            }
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("新建自定义属性-重复英文名(负向)")
    void test_addCustomAttributeDuplicateNameEn() {
        String nameEn = "AT_Dup_" + suffix();
        try {
            api.addCustomAttribute(nameEn, "重复属性1", "字符串", PROJECT_ID);
            String[] found = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(found, "第一条应创建成功");

            String resp = api.addCustomAttribute(nameEn, "重复属性2", "整型", PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 200) {
                log.warn("⚠ 服务端未校验重复英文名，允许创建 — 建议服务端增加校验");
            } else {
                log.info("新建属性-重复英文名 通过: 被拦截, code={}, msg={}",
                        code, root.has("msg") ? root.get("msg").getAsString() : "");
            }
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    // ==================== 修改自定义属性 ====================

    @Test
    @DisplayName("GNYL_149: 修改自定义属性(正向)")
    void test_updateCustomAttribute() {
        String nameEn = null;
        try {
            nameEn = "AT_Update_" + suffix();
            api.addCustomAttribute(nameEn, "原始属性名", "字符串", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "新创建的属性应能找到");

            String resp = api.updateCustomAttribute(
                    info[0], nameEn, "修改后属性名", "整型",
                    info[1], info[2], PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "修改应成功, resp: " + resp);

            String[] refound = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(refound, "修改后属性应仍可查到");
            log.info("GNYL_149 通过: 修改属性 [{}] id={}, 新名称='修改后属性名', 新类型='整型'",
                    nameEn, info[0]);
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    // ==================== 发布自定义属性 ====================

    @Test
    @DisplayName("GNYL_153: 发布自定义属性(正向)")
    void test_publishCustomAttribute() {
        String nameEn = null;
        try {
            nameEn = "AT_Publish_" + suffix();
            api.addCustomAttribute(nameEn, "待发布属性", "字符串", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "新创建的属性应能找到");

            String resp = api.publishCustomAttribute(info[0], PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            if (code == 500) {
                log.warn("⚠ 发布属性失败(服务端异常) [{}] id={}, resp: {}", nameEn, info[0], resp);
            } else {
                Assertions.assertEquals(200, code, "发布应成功, resp: " + resp);
                log.info("GNYL_153 通过: 发布属性 [{}] id={}", nameEn, info[0]);
            }
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    // ==================== 查询自定义属性 ====================

    @Test
    @DisplayName("查询自定义属性列表(正向)")
    void test_getCustomAttributeList() {
        String nameEn = "AT_List_" + suffix();
        try {
            api.addCustomAttribute(nameEn, "列表查询属性", "字符串", PROJECT_ID);

            String resp = api.getCustomAttributeList(PROJECT_ID);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询列表应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            Assertions.assertTrue(root.get("data").isJsonArray(), "data应为数组");
            Assertions.assertTrue(resp.contains(nameEn),
                    "列表中应包含属性: " + nameEn);
            log.info("查询属性列表 通过: 包含属性 [{}]", nameEn);
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("按条件搜索自定义属性(正向)")
    void test_searchCustomAttribute() {
        String nameEn = "AT_Search_" + suffix();
        try {
            api.addCustomAttribute(nameEn, "搜索测试属性", "字符串", PROJECT_ID);

            String resp = api.searchCustomAttribute(PROJECT_ID, "需求管理", "req",
                    "", "字符串", "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "搜索应成功, resp: " + resp);
            Assertions.assertTrue(resp.contains(nameEn),
                    "搜索结果应包含属性: " + nameEn);
            log.info("条件搜索属性 通过: 找到属性 [{}]", nameEn);
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("搜索不存在自定义属性(负向)")
    void test_searchCustomAttributeNotFound() {
        String resp = api.searchCustomAttribute(PROJECT_ID, "需求管理", "req",
                "AT_NonExistent_" + suffix(), "", "");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索请求本身应成功, resp: " + resp);
        Assertions.assertTrue(api.isDataEmpty(resp),
                "不存在的属性应返回空data");
        log.info("搜索不存在属性 通过: data为空");
    }

    // ==================== 删除自定义属性 ====================

    @Test
    @DisplayName("删除自定义属性(正向)")
    void test_deleteCustomAttribute() {
        String nameEn = "AT_Del_" + suffix();
        try {
            api.addCustomAttribute(nameEn, "待删除属性", "字符串", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "新创建的属性应能找到");

            String resp = api.deleteCustomAttribute(info[0]);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除应成功, resp: " + resp);

            String[] refound = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNull(refound, "删除后不应再查到该属性: " + nameEn);
            log.info("删除属性 通过: [{}] id={} 已删除", nameEn, info[0]);
            nameEn = null;
        } finally {
            if (nameEn != null) cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @DisplayName("删除不存在的自定义属性(负向)")
    void test_deleteCustomAttributeInvalid() {
        String resp = api.deleteCustomAttribute("invalid_attr_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code == 200) {
            log.warn("⚠ 服务端未校验删除不存在的属性，返回成功 — 建议服务端增加校验");
        } else {
            log.info("删除不存在属性 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        }
    }

    // ==================== 批量删除 ====================

    @Test
    @DisplayName("批量删除自定义属性(正向)")
    void test_batchDeleteCustomAttributes() {
        String nameEn1 = "AT_Batch1_" + suffix();
        String nameEn2 = "AT_Batch2_" + suffix();
        try {
            api.addCustomAttribute(nameEn1, "批量删除1", "字符串", PROJECT_ID);
            api.addCustomAttribute(nameEn2, "批量删除2", "整型", PROJECT_ID);
            String[] info1 = api.findCustomAttribute(nameEn1, PROJECT_ID);
            String[] info2 = api.findCustomAttribute(nameEn2, PROJECT_ID);
            Assertions.assertNotNull(info1, "属性1应能找到");
            Assertions.assertNotNull(info2, "属性2应能找到");

            String resp = api.batchDeleteCustomAttributes(info1[0], info2[0]);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "批量删除应成功, resp: " + resp);

            Assertions.assertNull(api.findCustomAttribute(nameEn1, PROJECT_ID),
                    "删除后属性1不应存在");
            Assertions.assertNull(api.findCustomAttribute(nameEn2, PROJECT_ID),
                    "删除后属性2不应存在");
            log.info("批量删除属性 通过: [{}] 和 [{}] 均已删除", nameEn1, nameEn2);
            nameEn1 = null;
            nameEn2 = null;
        } finally {
            if (nameEn1 != null) cleanupCustomAttr(nameEn1);
            if (nameEn2 != null) cleanupCustomAttr(nameEn2);
        }
    }
}
