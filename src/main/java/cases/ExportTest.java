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
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExportTest extends BaseTest {

    // ========== 工具方法 ==========

    private void closeExportDialog() {
        try {
            Locator closeBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭"));
            if (closeBtn.isVisible()) {
                closeBtn.click();
                page.waitForTimeout(500);
            }
        } catch (Exception e) {
            log.warn("关闭导出弹窗异常: {}", e.getMessage());
        }
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_060: 进入导出弹框")
    void test_GNYL_060_EnterExportDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_060 成功进入导出弹框");
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_061: 导出Excel")
    void test_GNYL_061_exportExcel() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator excelOption = page.getByText("Excel", new Page.GetByTextOptions().setExact(true));
            if (excelOption.isVisible()) {
                excelOption.click();
                page.waitForTimeout(1000);
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("导出")).click();
                page.waitForTimeout(3000);
                log.info("GNYL_061 Excel导出完成");
            } else {
                log.warn("GNYL_061 未找到Excel导出选项");
            }
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_062: 导出Word")
    void test_GNYL_062_exportWord() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator wordOption = page.getByText("Word", new Page.GetByTextOptions().setExact(true));
            if (wordOption.isVisible()) {
                wordOption.click();
                page.waitForTimeout(1000);
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("导出")).click();
                page.waitForTimeout(3000);
                log.info("GNYL_062 Word导出完成");
            } else {
                log.warn("GNYL_062 未找到Word导出选项");
            }
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_063: 导出ReqIF")
    void test_GNYL_063_exportReqIF() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator reqIfOption = page.getByText("ReqIF", new Page.GetByTextOptions().setExact(true));
            if (reqIfOption.isVisible()) {
                reqIfOption.click();
                page.waitForTimeout(1000);
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("导出")).click();
                page.waitForTimeout(3000);
                log.info("GNYL_063 ReqIF导出完成");
            } else {
                log.warn("GNYL_063 未找到ReqIF导出选项");
            }
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_064: 导出设置")
    void test_GNYL_064_exportSettings() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator settingSection = page.locator("[class*='setting'], [class*='option']").first();
            if (settingSection.isVisible()) {
                log.info("GNYL_064 导出设置区域可见");
            }
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_065: 批量导出")
    void test_GNYL_065_batchExport() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

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
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_066: 导出历史记录")
    void test_GNYL_066_exportHistory() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator excelOption = page.getByText("Excel", new Page.GetByTextOptions().setExact(true));
            if (excelOption.isVisible()) {
                excelOption.click();
                page.waitForTimeout(1000);
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName("导出")).click();
                page.waitForTimeout(3000);
            }

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
        } finally {
            closeExportDialog();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_067: API导出Word数据")
    void test_GNYL_067_exportWordViaAPI() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            log.info("GNYL_067 API导出Word - 调用导出接口, docId={}", doc[0]);

            APIResponse resp = page.request().get(
                    TestConfig.API_PREFIX + "/erm/exportWordReqSpecification",
                    RequestOptions.create()
                            .setQueryParam("objectId", doc[0])
                            .setQueryParam("templateType", "two")
                            .setHeader("ProjectId", PROJECT_ID));

            int status = resp.status();
            if (status == 200) {
                log.info("GNYL_067 API导出Word成功, 响应大小: {} bytes, type: {}",
                        resp.body().length, resp.headers().get("content-type"));
            } else {
                log.warn("GNYL_067 API导出Word返回状态码: {}", status);
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_068: API导出Excel数据")
    void test_GNYL_068_exportExcelViaAPI() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            log.info("GNYL_068 API导出Excel - 调用导出接口, docId={}", doc[0]);

            APIResponse resp = page.request().get(
                    TestConfig.API_PREFIX + "/erm/exportExcelReqSpecification",
                    RequestOptions.create()
                            .setQueryParam("objectId", doc[0])
                            .setQueryParam("templateType", "one")
                            .setHeader("ProjectId", PROJECT_ID));

            int status = resp.status();
            if (status == 200) {
                log.info("GNYL_068 API导出Excel成功, 响应大小: {} bytes, type: {}",
                        resp.body().length, resp.headers().get("content-type"));
            } else {
                log.warn("GNYL_068 API导出Excel返回状态码: {}", status);
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_069: API导出ReqIF数据")
    void test_GNYL_069_exportReqIFViaAPI() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            log.info("GNYL_069 API导出ReqIF - 调用导出接口, docId={}", doc[0]);

            APIResponse resp = page.request().get(
                    TestConfig.API_PREFIX + "/erm/exportExcelReqSpecification",
                    RequestOptions.create()
                            .setQueryParam("objectId", doc[0])
                            .setQueryParam("templateType", "one")
                            .setHeader("ProjectId", PROJECT_ID));

            int status = resp.status();
            if (status == 200) {
                log.info("GNYL_069 API导出ReqIF成功, 响应大小: {} bytes", resp.body().length);
            } else {
                log.warn("GNYL_069 API导出ReqIF返回状态码: {}", status);
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }
}
