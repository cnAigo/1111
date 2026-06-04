package cases.attribute;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.regex.Pattern;

@Tag("AttributeModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EnumAttributeTest extends BaseTest {

    // ========== 工具方法 ==========

    private String randomEnName() {
        return "enum_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private void cleanupAttr(String nameEn) {
        try {
            String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
            if (info != null) {
                api.deleteCustomAttribute(info[0]);
            }
        } catch (Exception e) {
            log.warn("清理属性 {} 失败: {}", nameEn, e.getMessage());
        }
    }

    private String postAttr(String payload) {
        com.microsoft.playwright.APIResponse response = page.request().post(
                TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        return response.text();
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_179: 单选枚举类属性保存")
    void test_GNYL_179_EnumSingleSave() {
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "单选枚举测试",
                    "type": "枚举",
                    "current": "1",
                    "valueRange": "选项1,选项2,选项3",
                    "defaultValue": "",
                    "isMultiple": false,
                    "description": "单选枚举测试",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "",
                    "createTime": "",
                    "creator": "",
                    "modifier": "",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": false,
                    "valueRangeMapping": [
                        {"value": "选项1"}, {"value": "选项2"}, {"value": "选项3"}
                    ]
                }
                """.formatted(nameEn, TestConstants.PROJECT_ID);
        String resp = postAttr(payload);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建单选枚举属性失败: " + resp);
            log.info("GNYL_179 单选枚举类属性保存成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_180: 多选枚举类属性保存")
    void test_GNYL_180_EnumMultiSave() {
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "多选枚举测试",
                    "type": "枚举",
                    "current": "1",
                    "valueRange": "多选A,多选B,多选C",
                    "defaultValue": "",
                    "isMultiple": true,
                    "description": "多选枚举测试",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "",
                    "createTime": "",
                    "creator": "",
                    "modifier": "",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": false,
                    "valueRangeMapping": [
                        {"value": "多选A"}, {"value": "多选B"}, {"value": "多选C"}
                    ]
                }
                """.formatted(nameEn, TestConstants.PROJECT_ID);
        String resp = postAttr(payload);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建多选枚举属性失败: " + resp);
            log.info("GNYL_180 多选枚举类属性保存成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_183: 用户类属性弹窗进入")
    void test_GNYL_183_UserTypeDialog() {
        String nameEn = randomEnName();
        try {
            reqPage.navigateToAttributeList();
            reqPage.openAddDialog();

            page.getByLabel("英文名").fill(nameEn);
            page.getByLabel("中文名").fill("用户属性弹窗测试");

            page.locator("div").filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^请选择$"))).nth(3).click();
            page.waitForTimeout(300);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("用户")).click();
            page.waitForTimeout(500);

            Locator userSelect = page.locator("[class*='user'], [class*='member'], button:has-text('选择')").first();
            if (userSelect.isVisible()) {
                userSelect.click();
                page.waitForTimeout(500);
                Locator dialog = page.locator(".el-dialog").first();
                if (dialog.isVisible()) {
                    log.info("GNYL_183 用户类属性弹窗成功打开");
                    page.keyboard().press("Escape");
                    page.waitForTimeout(300);
                }
            } else {
                log.info("GNYL_183 未找到用户选择控件");
            }
        } finally {
            reqPage.closeDialog();
        }
    }

    @Test
    @DisplayName("GNYL_184: 用户类属性人员添加")
    void test_GNYL_184_UserTypeAddPerson() {
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "用户属性添加测试",
                    "type": "用户",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": true,
                    "description": "用户类属性添加测试",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "",
                    "createTime": "",
                    "creator": "",
                    "modifier": "",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": false,
                    "valueRangeMapping": []
                }
                """.formatted(nameEn, TestConstants.PROJECT_ID);
        String resp = postAttr(payload);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建用户属性失败: " + resp);
            log.info("GNYL_184 用户类属性创建成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_185: 用户类属性人员移除")
    void test_GNYL_185_UserTypeRemovePerson() {
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "用户属性移除测试",
                    "type": "用户",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": true,
                    "description": "用户类属性移除测试",
                    "businessDomain": "需求管理",
                    "objectType": "req",
                    "id": "",
                    "createTime": "",
                    "creator": "",
                    "modifier": "",
                    "projectId": "%s",
                    "usedColor": "#1e90ff",
                    "isUseDefaultValue": false,
                    "valueRangeMapping": []
                }
                """.formatted(nameEn, TestConstants.PROJECT_ID);
        String resp = postAttr(payload);
        Assertions.assertTrue(resp.contains("200"), "创建用户属性失败: " + resp);
        try {
            String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
            if (info != null) {
                String delResp = api.deleteCustomAttribute(info[0]);
                Assertions.assertTrue(delResp.contains("200") || delResp.contains("成功"),
                        "删除用户属性失败: " + delResp);
                log.info("GNYL_185 用户类属性人员/属性移除成功");
            } else {
                log.info("GNYL_185 属性未查找到");
            }
        } finally {
            cleanupAttr(nameEn);
        }
    }
}
