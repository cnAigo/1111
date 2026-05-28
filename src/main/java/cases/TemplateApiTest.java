package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TemplateApiTest extends BaseTest {

    @Test
    @DisplayName("新建模板(正向)")
    void test_insertTemplate() {
        String templateName = "AT_Template_" + suffix();
        String attrList = """
                [
                    {"attrName": "AT_str_%s", "attrType": "整型", "valueRange": null},
                    {"attrName": "AT_flt_%s", "attrType": "浮点", "valueRange": null}
                ]
                """.formatted(suffix(), suffix());

        String resp = api.insertTemplate(templateName, PROJECT_ID,
                "自动化测试模板", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "新建模板应成功, resp: " + resp);
        log.info("新建模板 通过: [{}]", templateName);
    }

    @Test
    @DisplayName("新建模板-空名称(负向)")
    void test_insertTemplateEmptyName() {
        String attrList = """
                [{"attrName": "test", "attrType": "整型", "valueRange": null}]
                """;

        String resp = api.insertTemplate("", PROJECT_ID,
                "空名称模板", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "空模板名称应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("新建模板-空名称 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("新建模板-空项目ID(负向)")
    void test_insertTemplateEmptyProjectId() {
        String attrList = """
                [{"attrName": "test", "attrType": "整型", "valueRange": null}]
                """;

        String resp = api.insertTemplate("AT_Template_" + suffix(), "",
                "空项目ID", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "空项目ID应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("新建模板-空项目ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("新建模板-空属性列表(负向)")
    void test_insertTemplateEmptyAttrList() {
        String resp = api.insertTemplate("AT_Template_" + suffix(), PROJECT_ID,
                "空属性列表", "[]");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("新建模板-空属性列表: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("新建模板-超长名称(负向)")
    void test_insertTemplateTooLongName() {
        String longName = "T".repeat(200);
        String attrList = """
                [{"attrName": "test", "attrType": "整型", "valueRange": null}]
                """;

        String resp = api.insertTemplate(longName, PROJECT_ID,
                "超长名称模板", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("新建模板-超长名称: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("新建模板-无效项目ID(负向)")
    void test_insertTemplateInvalidProjectId() {
        String attrList = """
                [{"attrName": "test", "attrType": "整型", "valueRange": null}]
                """;

        String resp = api.insertTemplate("AT_Template_" + suffix(),
                "invalid_project_99999", "无效项目", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "无效项目ID应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("新建模板-无效项目ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("新建模板-空模板描述(负向)")
    void test_insertTemplateEmptyDesc() {
        String attrList = """
                [{"attrName": "test", "attrType": "整型", "valueRange": null}]
                """;

        String resp = api.insertTemplate("AT_Template_" + suffix(), PROJECT_ID,
                "", attrList);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("新建模板-空描述: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
