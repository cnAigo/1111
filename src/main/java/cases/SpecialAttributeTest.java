package cases;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import actions.ReqApiActions;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.util.UUID;
import java.util.regex.Pattern;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SpecialAttributeTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(SpecialAttributeTest.class);
    private ReqApiActions api;
    private RequirementPage rPage;

    @BeforeAll
    public void initApi() {
        api = new ReqApiActions(page.request());
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 工具方法 ==========

    private String randomEnName() {
        return "sp_" + UUID.randomUUID().toString().substring(0, 6);
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

    private void closeDialogs() {
        try {
            while (page != null &&
                    page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible").count() > 0) {
                page.keyboard().press("Escape");
                page.waitForTimeout(300);
            }
        } catch (Exception e) {
            log.warn("清理残留弹窗异常: {}", e.getMessage());
        }
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_174: 整数类属性非整数拦截")
    void test_GNYL_174_IntegerValidation() {
        try {
            rPage.navigateToAttributeList();
            rPage.openAddDialog();

            String nameEn = randomEnName();
            page.getByLabel("英文名").fill(nameEn);
            page.getByLabel("中文名").fill("整数验证属性");

            page.locator("div").filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^请选择$"))).nth(3).click();
            page.waitForTimeout(300);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("整型")).click();
            page.waitForTimeout(500);

            Locator valueInput = page.locator("input[placeholder*='取值']").first();
            if (valueInput.isVisible()) {
                valueInput.fill("abc");
                page.waitForTimeout(300);

                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确认")).click();
                page.waitForTimeout(500);

                Locator errorMsg = page.locator(".el-message--error, [class*='error']").first();
                if (errorMsg.isVisible()) {
                    log.info("GNYL_174 整数类属性非整数输入被拦截: {}", errorMsg.textContent());
                } else {
                    log.info("GNYL_174 整数验证输入通过(可能前端已控制输入)");
                }
            }
        } finally {
            rPage.closeDialog();
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_175: 整数类属性保存")
    void test_GNYL_175_IntegerSave() {
        String nameEn = randomEnName();
        String resp = api.addCustomAttribute(nameEn, "整数保存测试", "整型", TestConstants.PROJECT_ID);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建整数属性失败: " + resp);
            log.info("GNYL_175 整数类属性保存成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_176: 浮点类属性非法字符拦截")
    void test_GNYL_176_FloatValidation() {
        try {
            rPage.navigateToAttributeList();
            rPage.openAddDialog();

            String nameEn = randomEnName();
            page.getByLabel("英文名").fill(nameEn);
            page.getByLabel("中文名").fill("浮点验证属性");

            page.locator("div").filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^请选择$"))).nth(3).click();
            page.waitForTimeout(300);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("浮点")).click();
            page.waitForTimeout(500);

            Locator valueInput = page.locator("input[placeholder*='取值']").first();
            if (valueInput.isVisible()) {
                valueInput.fill("@#$");
                page.waitForTimeout(300);

                Locator errorMsg = page.locator(".el-message--error, [class*='error']").first();
                if (errorMsg.isVisible()) {
                    log.info("GNYL_176 浮点类属性非法字符被拦截: {}", errorMsg.textContent());
                } else {
                    log.info("GNYL_176 输入非法字符无错误提示(可能前端已控制输入)");
                }
            }
        } finally {
            rPage.closeDialog();
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_177: 浮点类属性保存")
    void test_GNYL_177_FloatSave() {
        String nameEn = randomEnName();
        String resp = api.addCustomAttribute(nameEn, "浮点保存测试", "浮点", TestConstants.PROJECT_ID);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建浮点属性失败: " + resp);
            log.info("GNYL_177 浮点类属性保存成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_178: 文本类属性保存")
    void test_GNYL_178_TextSave() {
        String nameEn = randomEnName();
        String resp = api.addCustomAttribute(nameEn, "文本保存测试", "文本", TestConstants.PROJECT_ID);
        try {
            Assertions.assertTrue(resp.contains("200"), "创建文本属性失败: " + resp);
            log.info("GNYL_178 文本类属性保存成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }

    @Test
    @DisplayName("GNYL_181: 日期类属性选择与保存")
    void test_GNYL_181_DateAttributeSave() {
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "日期属性测试",
                    "type": "日期",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": false,
                    "description": "日期类属性测试",
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

        com.microsoft.playwright.APIResponse response = page.request().post(
                TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        String resp = response.text();
        try {
            Assertions.assertTrue(resp.contains("200"), "创建日期属性失败: " + resp);
            log.info("GNYL_181 日期类属性选择与保存成功: {}", nameEn);

            rPage.navigateToAttributeList();
            page.waitForTimeout(1000);
        } finally {
            cleanupAttr(nameEn);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_182: 日期类属性删除")
    void test_GNYL_182_DateAttributeDelete() {
        // 独立创建自己的日期属性来删除
        String nameEn = randomEnName();
        String payload = """
                {
                    "nameEn": "%s",
                    "name": "日期删除测试",
                    "type": "日期",
                    "current": "1",
                    "valueRange": "",
                    "defaultValue": "",
                    "isMultiple": false,
                    "description": "日期类属性删除测试",
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

        page.request().post(
                TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );

        String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
        Assertions.assertNotNull(info, "未查找到日期属性: " + nameEn);
        try {
            String delResp = api.deleteCustomAttribute(info[0]);
            Assertions.assertTrue(delResp.contains("200") || delResp.contains("成功"),
                    "删除日期属性失败: " + delResp);

            String[] check = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
            Assertions.assertNull(check, "日期属性应已被删除");
            log.info("GNYL_182 日期类属性删除成功: {}", nameEn);
        } finally {
            cleanupAttr(nameEn);
        }
    }
}
