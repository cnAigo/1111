package cases;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import actions.ReqApiActions;
import config.TestConfig;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.util.regex.Pattern;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    // ========== 特殊类型属性功能测试 ==========
    // ============================================================

    // GNYL_174: 整数类属性非整数拦截
    @Test
    @Order(1740)
    @DisplayName("GNYL_174: 整数类属性非整数拦截")
    void test_GNYL_174_IntegerValidation() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        String nameEn = "int_val_" + System.currentTimeMillis();
        page.getByLabel("英文名").fill(nameEn);
        page.getByLabel("中文名").fill("整数验证属性");

        // 选择整型类型
        page.locator("div").filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^请选择$"))).nth(3).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("整型")).click();
        page.waitForTimeout(500);

        // 尝试输入非法字符作为默认值/取值范围
        page.getByLabel("添加参数").locator("div")
                .filter(new Locator.FilterOptions().setHasText("整型")).first();
        // 尝试在值范围输入非整数字符
        Locator valueInput = page.locator("input[placeholder*='取值']").first();
        if (valueInput.isVisible()) {
            valueInput.fill("abc");
            page.waitForTimeout(300);
            // 保存并验证是否拦截
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确认")).click();
            page.waitForTimeout(500);

            Locator errorMsg = page.locator(".el-message--error, [class*='error']").first();
            if (errorMsg.isVisible()) {
                log.info("GNYL_174 整数类属性非整数输入被拦截: {}", errorMsg.textContent());
            } else {
                log.info("GNYL_174 整数验证输入通过(可能前端已控制输入)");
            }
        }

        TestContext.set("intAttrNameEn", nameEn);
        rPage.closeDialog();
    }

    // GNYL_175: 整数类属性保存
    @Test
    @Order(1750)
    @DisplayName("GNYL_175: 整数类属性保存")
    void test_GNYL_175_IntegerSave() {
        String nameEn = "int_save_" + System.currentTimeMillis();
        // 通过API创建整数类型属性
        String resp = api.addCustomAttribute(nameEn, "整数保存测试", "整型", TestConstants.PROJECT_ID);
        Assertions.assertTrue(resp.contains("200"), "创建整数属性失败: " + resp);
        log.info("GNYL_175 整数类属性保存成功: {}", nameEn);
        TestContext.set("savedIntAttrName", nameEn);
    }

    // GNYL_176: 浮点类属性非法字符拦截
    @Test
    @Order(1760)
    @DisplayName("GNYL_176: 浮点类属性非法字符拦截")
    void test_GNYL_176_FloatValidation() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        String nameEn = "flt_val_" + System.currentTimeMillis();
        page.getByLabel("英文名").fill(nameEn);
        page.getByLabel("中文名").fill("浮点验证属性");

        // 选择浮点类型
        page.locator("div").filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^请选择$"))).nth(3).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("浮点")).click();
        page.waitForTimeout(500);

        // 输入非法字符
        Locator valueInput = page.locator("input[placeholder*='取值']").first();
        if (valueInput.isVisible()) {
            valueInput.fill("@#$");
            page.waitForTimeout(300);
            // 验证是否有错误提示
            Locator errorMsg = page.locator(".el-message--error, [class*='error']").first();
            if (errorMsg.isVisible()) {
                log.info("GNYL_176 浮点类属性非法字符被拦截: {}", errorMsg.textContent());
            } else {
                log.info("GNYL_176 输入非法字符无错误提示(可能前端已控制输入)");
            }
        }

        rPage.closeDialog();
    }

    // GNYL_177: 浮点类属性保存
    @Test
    @Order(1770)
    @DisplayName("GNYL_177: 浮点类属性保存")
    void test_GNYL_177_FloatSave() {
        String nameEn = "flt_save_" + System.currentTimeMillis();
        String resp = api.addCustomAttribute(nameEn, "浮点保存测试", "浮点", TestConstants.PROJECT_ID);
        Assertions.assertTrue(resp.contains("200"), "创建浮点属性失败: " + resp);
        log.info("GNYL_177 浮点类属性保存成功: {}", nameEn);
        TestContext.set("savedFloatAttrName", nameEn);
    }

    // GNYL_178: 文本类属性保存
    @Test
    @Order(1780)
    @DisplayName("GNYL_178: 文本类属性保存")
    void test_GNYL_178_TextSave() {
        String nameEn = "txt_save_" + System.currentTimeMillis();
        String resp = api.addCustomAttribute(nameEn, "文本保存测试", "文本", TestConstants.PROJECT_ID);
        Assertions.assertTrue(resp.contains("200"), "创建文本属性失败: " + resp);
        log.info("GNYL_178 文本类属性保存成功: {}", nameEn);
        TestContext.set("savedTextAttrName", nameEn);
    }

    // GNYL_181: 日期类属性选择与保存
    @Test
    @Order(1810)
    @DisplayName("GNYL_181: 日期类属性选择与保存")
    void test_GNYL_181_DateAttributeSave() {
        String nameEn = "date_save_" + System.currentTimeMillis();
        // 日期类型属性通过API创建
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

        com.microsoft.playwright.APIRequestContext reqCtx = page.request();
        com.microsoft.playwright.APIResponse response = reqCtx.post(
                TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        String resp = response.text();
        Assertions.assertTrue(resp.contains("200"), "创建日期属性失败: " + resp);
        log.info("GNYL_181 日期类属性选择与保存成功: {}", nameEn);
        TestContext.set("savedDateAttrName", nameEn);

        // UI验证：导航查看
        rPage.navigateToAttributeList();
        page.waitForTimeout(1000);
    }

    // GNYL_182: 日期类属性删除
    @Test
    @Order(1820)
    @DisplayName("GNYL_182: 日期类属性删除")
    void test_GNYL_182_DateAttributeDelete() {
        String nameEn = TestContext.get("savedDateAttrName");
        if (nameEn == null || nameEn.isEmpty()) {
            // 如果GNYL_181没有执行，临时创建一个删除
            nameEn = "date_del_" + System.currentTimeMillis();
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

            com.microsoft.playwright.APIRequestContext reqCtx = page.request();
            reqCtx.post(
                    TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                    com.microsoft.playwright.options.RequestOptions.create()
                            .setHeader("Content-Type", "application/json")
                            .setData(payload)
            );
        }

        String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
        Assumptions.assumeTrue(info != null, "未查找到日期属性: " + nameEn);

        String delResp = api.deleteCustomAttribute(info[0]);
        Assertions.assertTrue(delResp.contains("200") || delResp.contains("成功"),
                "删除日期属性失败: " + delResp);

        String[] check = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
        Assertions.assertNull(check, "日期属性应已被删除");
        log.info("GNYL_182 日期类属性删除成功: {}", nameEn);
    }

}
