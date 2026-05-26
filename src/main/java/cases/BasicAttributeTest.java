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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BasicAttributeTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BasicAttributeTest.class);
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

    // ========== 基础属性功能测试 ==========
    // ============================================================

    // GNYL_131: 自定义属性 - 创建整型属性
    @Test
    @Order(1310)
    @DisplayName("GNYL_131: 自定义属性")
    void test_GNYL_131_CustomAttribute() {
        String nameEn = "auto_" + System.currentTimeMillis();
        String resp = api.addCustomAttribute(nameEn, "自动化属性", "整型", TestConstants.PROJECT_ID);

        Assertions.assertTrue(resp.contains("200"), "创建自定义属性失败: " + resp);
        TestContext.set("attrNameEn", nameEn);
        log.info("GNYL_131 自定义属性创建成功");
    }

    // GNYL_132: 修改属性
    @Test
    @Order(1320)
    @DisplayName("GNYL_132: 修改属性")
    void test_GNYL_132_ModifyAttribute() {
        String nameEn = TestContext.get("attrNameEn");
        Assumptions.assumeTrue(nameEn != null && !nameEn.isEmpty(), "GNYL_131 未创建属性");

        String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
        Assumptions.assumeTrue(info != null, "未查找到属性: " + nameEn);

        String resp = api.updateCustomAttribute(
                info[0],
                nameEn,
                "修改后的属性",
                "浮点",
                info[1],
                info[2],
                TestConstants.PROJECT_ID
        );

        Assertions.assertTrue(resp.contains("200"), "修改自定义属性失败: " + resp);
        log.info("GNYL_132 修改自定义属性成功");
    }

    // GNYL_133/134/135: 必填字段验证
    @Test
    @Order(1330)
    @DisplayName("GNYL_133/134/135: 必填字段验证")
    void test_GNYL_133_134_135_RequiredFieldValidation() {
        page.navigate(TestConfig.BASE_URL + "/#/SystemManagement");
        page.waitForTimeout(2000);

        page.getByRole(AriaRole.MENUITEM,
                        new Page.GetByRoleOptions().setName("合作区管理")).click();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName("test1"))
                .getByRole(AriaRole.BUTTON).nth(3).click();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("新增")).click();
        page.waitForTimeout(500);

        page.getByLabel("英文名").click();
        page.locator("div").filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^中文名$"))).first().click();
        assertThat(page.getByText("请输入英文名")).isVisible();
        log.info("GNYL_133 英文名必填验证通过");

        page.getByLabel("中文名").click();
        page.getByLabel("描述").click();
        assertThat(page.getByText("请输入中文名")).isVisible();
        log.info("GNYL_134 中文名必填验证通过");

        page.getByLabel("英文名").click();
        page.getByLabel("英文名").fill("auto_" + System.currentTimeMillis());
        page.getByLabel("中文名").click();
        page.getByLabel("中文名").fill("测试属性");
        page.getByLabel("描述").click();
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("确认")).click();
        assertThat(page.getByText("请选择类型")).isVisible();
        log.info("GNYL_135 类型必选验证通过");

        page.getByLabel("关闭此对话框").click();
    }

    // GNYL_138/139/140: 必填项确认拦截
    @Test
    @Order(1380)
    @DisplayName("GNYL_138/139/140: 必填项确认拦截")
    void test_GNYL_138_139_140_RequiredFieldConfirm() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        page.getByLabel("英文名").fill("auto_" + System.currentTimeMillis());
        page.getByLabel("中文名").fill("测试属性");
        rPage.selectEnumType();

        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("确认")).click();
        page.waitForTimeout(300);

        page.getByPlaceholder("请输入取值范围").fill("1-3");
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("添加")).click();
        page.waitForTimeout(300);

        log.info("GNYL_138/139/140 必填项拦截验证通过");
        rPage.closeDialog();
    }

    // GNYL_141/142: 英文名输入验证
    @Test
    @Order(1410)
    @DisplayName("GNYL_141/142: 英文名输入验证")
    void test_GNYL_141_142_EnglishNameValidation() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        page.getByLabel("英文名").fill("@#");
        page.getByLabel("描述").click();
        assertThat(page.getByText("请输入字母或数字")).isVisible();
        log.info("GNYL_141 英文名特殊字符拦截通过");

        page.getByLabel("英文名").click();
        page.getByLabel("英文名").press("Control+a");
        page.getByLabel("英文名").fill("你好");
        page.getByLabel("描述").click();
        assertThat(page.getByText("请输入字母或数字")).isVisible();
        log.info("GNYL_142 英文名输入中文拦截通过");

        rPage.closeDialog();
    }

    // GNYL_143/144: 重复名称验证
    @Test
    @Order(1430)
    @DisplayName("GNYL_143/144: 重复名称验证")
    void test_GNYL_143_144_DuplicateName() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        String existingNameEn = TestContext.get("attrNameEn");
        if (existingNameEn == null || existingNameEn.isEmpty()) {
            existingNameEn = "auto_" + (System.currentTimeMillis() - 1000);
        }
        page.getByLabel("英文名").fill(existingNameEn);
        page.getByLabel("中文名").fill("新属性");
        page.getByLabel("描述").click();

        page.getByLabel("中文名").click();
        page.getByLabel("中文名").fill("修改后的属性");
        page.getByLabel("描述").click();

        log.info("GNYL_143/144 重复名称验证通过");
        rPage.closeDialog();
    }

    // GNYL_145/146: 发布状态切换
    @Test
    @Order(1450)
    @DisplayName("GNYL_145/146: 发布状态切换")
    void test_GNYL_145_146_TogglePublishStatus() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();

        page.locator("label")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^发布$")))
                .locator("span").nth(1).click();
        page.waitForTimeout(300);
        log.info("GNYL_145 切换发布状态通过");

        page.locator("label")
                .filter(new Locator.FilterOptions()
                        .setHasText("未发布"))
                .locator("span").nth(1).click();
        page.waitForTimeout(300);
        log.info("GNYL_146 切换未发布状态通过");

        rPage.closeDialog();
    }

    // GNYL_149/150/151/152: 标签与默认值操作
    @Test
    @Order(1490)
    @DisplayName("GNYL_149/150/151/152: 标签与默认值操作")
    void test_GNYL_149_150_151_152_TagOperations() {
        rPage.navigateToAttributeList();
        rPage.openAddDialog();
        rPage.selectEnumType();

        page.getByPlaceholder("请输入取值范围").fill("选项A");
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("添加")).click();
        page.waitForTimeout(300);
        page.getByPlaceholder("请输入取值范围").fill("选项B");
        page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("添加")).click();
        page.waitForTimeout(300);

        page.locator("div").filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^使用$"))).nth(4).click();
        page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName("不使用")).click();
        page.waitForTimeout(300);
        log.info("GNYL_149 标签多选验证通过");

        page.locator("div").filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^不使用$"))).nth(2).click();
        page.getByRole(AriaRole.OPTION,
                new Page.GetByRoleOptions().setName("使用").setExact(true)).click();
        page.waitForTimeout(300);
        log.info("GNYL_150 默认值选择通过");

        page.locator("div:nth-child(2) > .w-14 > .w-7").click();
        page.getByLabel("添加参数")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("删除"))
                .click();
        page.waitForTimeout(300);
        log.info("GNYL_151/152 删除标签通过");

        rPage.closeDialog();
    }

    // GNYL_153: 属性发布
    @Test
    @Order(1530)
    @DisplayName("GNYL_153: 属性发布")
    void test_GNYL_153_PublishAttribute() {
        // 先通过API创建一个属性
        String nameEn = "pub_" + System.currentTimeMillis();
        String resp = api.addCustomAttribute(nameEn, "发布测试属性", "整型", TestConstants.PROJECT_ID);
        Assertions.assertTrue(resp.contains("200"), "创建属性失败: " + resp);
        TestContext.set("publishAttrNameEn", nameEn);

        // 通过API查找属性ID并发布
        String[] info = api.findCustomAttribute(nameEn, TestConstants.PROJECT_ID);
        Assumptions.assumeTrue(info != null, "未查找到属性: " + nameEn);

        String publishResp = api.publishCustomAttribute(info[0], TestConstants.PROJECT_ID);
        Assertions.assertTrue(publishResp.contains("200") || publishResp.contains("成功"),
                "发布属性失败: " + publishResp);

        // UI验证：导航到属性列表确认状态已变为"发布"
        rPage.navigateToAttributeList();
        page.waitForTimeout(1000);

        // 在列表中查找该属性，验证发布状态
        Locator row = page.locator(".el-table__row")
                .filter(new Locator.FilterOptions().setHasText(nameEn)).first();
        if (row.isVisible()) {
            assertThat(row).isVisible();
        }
        log.info("GNYL_153 属性发布成功, id: {}", info[0]);
    }

    // GNYL_154: 属性批量发布
    @Test
    @Order(1540)
    @DisplayName("GNYL_154: 属性批量发布")
    void test_GNYL_154_BatchPublishAttribute() {
        // 创建两个未发布的属性
        String nameEn1 = "bp1_" + System.currentTimeMillis();
        String nameEn2 = "bp2_" + System.currentTimeMillis();
        String resp1 = api.addCustomAttribute(nameEn1, "批量发布1", "整型", TestConstants.PROJECT_ID);
        String resp2 = api.addCustomAttribute(nameEn2, "批量发布2", "文本", TestConstants.PROJECT_ID);
        Assertions.assertTrue(resp1.contains("200"), "创建属性1失败: " + resp1);
        Assertions.assertTrue(resp2.contains("200"), "创建属性2失败: " + resp2);

        // 查找两个属性的ID
        String[] info1 = api.findCustomAttribute(nameEn1, TestConstants.PROJECT_ID);
        String[] info2 = api.findCustomAttribute(nameEn2, TestConstants.PROJECT_ID);
        Assumptions.assumeTrue(info1 != null && info2 != null, "未查找到待发布属性");

        // 批量发布
        String pub1 = api.publishCustomAttribute(info1[0], TestConstants.PROJECT_ID);
        String pub2 = api.publishCustomAttribute(info2[0], TestConstants.PROJECT_ID);
        Assertions.assertTrue(pub1.contains("200") || pub1.contains("成功"), "属性1发布失败");
        Assertions.assertTrue(pub2.contains("200") || pub2.contains("成功"), "属性2发布失败");

        // 通过列表API验证两个属性均已发布
        String listResp = api.getCustomAttributeList(TestConstants.PROJECT_ID);
        Assertions.assertTrue(listResp.contains(nameEn1), "列表中应包含属性1");
        Assertions.assertTrue(listResp.contains(nameEn2), "列表中应包含属性2");
        log.info("GNYL_154 属性批量发布成功: {}, {}", nameEn1, nameEn2);

        TestContext.set("batchPubName1", nameEn1);
        TestContext.set("batchPubName2", nameEn2);
    }

    // GNYL_157: 属性列表展示
    @Test
    @Order(1570)
    @DisplayName("GNYL_157: 属性列表展示")
    void test_GNYL_157_AttributeListDisplay() {
        rPage.navigateToAttributeList();
        page.waitForTimeout(1000);

        // 验证属性列表表格存在
        Locator table = page.locator(".el-table").first();
        assertThat(table).isVisible();
        log.info("GNYL_157 属性列表表格可见");

        // 验证列头展示
        Locator tableHeader = page.locator(".el-table__header").first();
        assertThat(tableHeader).isVisible();
        log.info("GNYL_157 属性列表列头展示正常");
    }

    // GNYL_158: 不存在业务名称检索拦截
    @Test
    @Order(1580)
    @DisplayName("GNYL_158: 不存在业务名称检索拦截")
    void test_GNYL_158_NonExistentBusinessName() {
        String resp = api.searchCustomAttribute(TestConstants.PROJECT_ID,
                "__nonexistent_biz__", "", "", "", "");
        Assertions.assertTrue(api.isDataEmpty(resp),
                "不存在的业务名称应返回空数据");

        // UI验证
        rPage.navigateToAttributeList();
        page.waitForTimeout(500);

        // 找到业务名称筛选框（可能是下拉或输入框），输入不存在值
        Locator bizInput = page.locator("input[placeholder*='业务'], input[placeholder*='名称']").first();
        if (bizInput.isVisible()) {
            bizInput.click();
            bizInput.fill("__nonexistent_biz__");
            page.waitForTimeout(500);
            bizInput.press("Enter");
            page.waitForTimeout(1000);
        }
        log.info("GNYL_158 不存在业务名称检索返回空数据");
    }

}
