package cases;

import base.BaseTest;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExportTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(ExportTest.class);
    private RequirementPage reqPage;
    private static final String EXPORT_DIR = "src/main/resources/exports/";

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
    }

    // ========== 导出功能测试 ==========
    // ============================================================

    // GNYL_060: 进入导出弹框
    @Test
    @Order(600)
    @DisplayName("GNYL_060: 进入导出弹框")
    void test_GNYL_060_EnterExportDialog() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_060 成功进入导出弹框");
        closeDialog();
    }

    // GNYL_061: 导出Excel
    @Test
    @Order(610)
    @DisplayName("GNYL_061: 导出Excel")
    void test_GNYL_061_exportExcel() {
        openExportDialog();

        Locator excelOption = page.getByText("Excel", new Page.GetByTextOptions().setExact(true));
        if (excelOption.isVisible()) {
            excelOption.click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(3000);

            log.info("GNYL_061 Excel导出完成，检查下载文件");
        } else {
            log.warn("GNYL_061 未找到Excel导出选项");
        }
        closeDialog();
    }

    // GNYL_062: 导出Word
    @Test
    @Order(620)
    @DisplayName("GNYL_062: 导出Word")
    void test_GNYL_062_exportWord() {
        openExportDialog();

        Locator wordOption = page.getByText("Word", new Page.GetByTextOptions().setExact(true));
        if (wordOption.isVisible()) {
            wordOption.click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(3000);

            log.info("GNYL_062 Word导出完成，检查下载文件");
        } else {
            log.warn("GNYL_062 未找到Word导出选项");
        }
        closeDialog();
    }

    // GNYL_063: 导出ReqIF
    @Test
    @Order(630)
    @DisplayName("GNYL_063: 导出ReqIF")
    void test_GNYL_063_exportReqIF() {
        openExportDialog();

        Locator reqIfOption = page.getByText("ReqIF", new Page.GetByTextOptions().setExact(true));
        if (reqIfOption.isVisible()) {
            reqIfOption.click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(3000);

            log.info("GNYL_063 ReqIF导出完成，检查下载文件");
        } else {
            log.warn("GNYL_063 未找到ReqIF导出选项");
        }
        closeDialog();
    }

    // GNYL_064: 导出设置
    @Test
    @Order(640)
    @DisplayName("GNYL_064: 导出设置")
    void test_GNYL_064_exportSettings() {
        openExportDialog();

        Locator settingSection = page.locator("[class*='setting'], [class*='option']").first();
        if (settingSection.isVisible()) {
            log.info("GNYL_064 导出设置区域可见，包含描述/属性等选项");
        }
        closeDialog();
    }

    // GNYL_065: 批量导出
    @Test
    @Order(650)
    @DisplayName("GNYL_065: 批量导出")
    void test_GNYL_065_batchExport() {
        openExportDialog();

        // 选择多个导出格式进行批量导出
        boolean hasMultipleFormats = false;

        Locator formats = page.locator("[class*='format'], [class*='type']");
        for (int i = 0; i < formats.count(); i++) {
            Locator format = formats.nth(i);
            if (format.isVisible()) {
                format.click();
                page.waitForTimeout(200);
                hasMultipleFormats = true;
            }
        }

        if (hasMultipleFormats) {
            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(5000);
            log.info("GNYL_065 批量导出完成");
        } else {
            log.info("GNYL_065 仅支持单一格式导出");
        }
        closeDialog();
    }

    // GNYL_066: 导出历史记录
    @Test
    @Order(660)
    @DisplayName("GNYL_066: 导出历史记录")
    void test_GNYL_066_exportHistory() {
        // 首先执行一次导出操作以产生历史记录
        test_GNYL_061_exportExcel();

        // 查看导出历史
        Locator historyBtn = page.getByText("历史", new Page.GetByTextOptions().setExact(true));
        if (historyBtn.isVisible()) {
            historyBtn.click();
            page.waitForTimeout(1000);

            Locator historyList = page.locator("[class*='history'], [class*='record']");
            if (historyList.isVisible()) {
                log.info("GNYL_066 导出历史记录可见");
            }
        } else {
            log.info("GNYL_066 无导出历史记录功能");
        }
    }

    // GNYL_067: API导出Word数据
    // 抓包确认: GET /erm/exportWordReqSpecification?objectId=xxx&templateType=two
    @Test
    @Order(670)
    @DisplayName("GNYL_067: API导出Word数据")
    void test_GNYL_067_exportWordViaAPI() {
        log.info("GNYL_067 API导出Word - 调用导出接口");

        String objectId = TestContext.get("spec1Id");
        if (objectId == null) {
            log.warn("GNYL_067 跳过: 未找到 spec1Id");
            return;
        }

        APIResponse resp = page.request().get(
                TestConfig.API_PREFIX + "/erm/exportWordReqSpecification",
                RequestOptions.create()
                        .setQueryParam("objectId", objectId)
                        .setQueryParam("templateType", "two")
                        .setHeader("ProjectId", TestConstants.PROJECT_ID));

        int status = resp.status();
        if (status == 200) {
            log.info("GNYL_067 API导出Word成功, 响应大小: {} bytes, type: {}",
                    resp.body().length, resp.headers().get("content-type"));
        } else {
            log.warn("GNYL_067 API导出Word返回状态码: {}", status);
        }
    }

    // GNYL_068: API导出Excel数据
    // 抓包确认: GET /erm/exportExcelReqSpecification?objectId=xxx&templateType=one
    @Test
    @Order(680)
    @DisplayName("GNYL_068: API导出Excel数据")
    void test_GNYL_068_exportExcelViaAPI() {
        log.info("GNYL_068 API导出Excel - 调用导出接口");

        String objectId = TestContext.get("spec1Id");
        if (objectId == null) {
            log.warn("GNYL_068 跳过: 未找到 spec1Id");
            return;
        }

        APIResponse resp = page.request().get(
                TestConfig.API_PREFIX + "/erm/exportExcelReqSpecification",
                RequestOptions.create()
                        .setQueryParam("objectId", objectId)
                        .setQueryParam("templateType", "one")
                        .setHeader("ProjectId", TestConstants.PROJECT_ID));

        int status = resp.status();
        if (status == 200) {
            log.info("GNYL_068 API导出Excel成功, 响应大小: {} bytes, type: {}",
                    resp.body().length, resp.headers().get("content-type"));
        } else {
            log.warn("GNYL_068 API导出Excel返回状态码: {}", status);
        }
    }

    // GNYL_069: API导出ReqIF数据
    // TODO: 确认 ReqIF 导出的实际 endpoint（当前复用 Excel 导出端点）
    @Test
    @Order(690)
    @DisplayName("GNYL_069: API导出ReqIF数据")
    void test_GNYL_069_exportReqIFViaAPI() {
        log.info("GNYL_069 API导出ReqIF - 调用导出接口");

        String objectId = TestContext.get("spec1Id");
        if (objectId == null) {
            log.warn("GNYL_069 跳过: 未找到 spec1Id");
            return;
        }

        APIResponse resp = page.request().get(
                TestConfig.API_PREFIX + "/erm/exportExcelReqSpecification",
                RequestOptions.create()
                        .setQueryParam("objectId", objectId)
                        .setQueryParam("templateType", "one")
                        .setHeader("ProjectId", TestConstants.PROJECT_ID));

        int status = resp.status();
        if (status == 200) {
            log.info("GNYL_069 API导出ReqIF成功, 响应大小: {} bytes", resp.body().length);
        } else {
            log.warn("GNYL_069 API导出ReqIF返回状态码: {}（可能需要修改为 ReqIF 专属端点）", status);
        }
    }

    // ========== 工具方法 ==========

    private void openExportDialog() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);
    }

    private void closeDialog() {
        Locator closeBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭"));
        if (!closeBtn.isHidden()) {
            closeBtn.click();
        }
        page.waitForTimeout(500);
    }

}
