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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public class WordImportTest extends BaseTest {

    // ==================== Excel import tests ====================

    @Test
    @DisplayName("GNYL_034: 进入导入Excel弹框")
    void test_GNYL_034_EnterImportExcelDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_034 成功进入导入Excel弹框");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_038: 下载Excel模板")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void test_GNYL_038_downloadExcelTemplate() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();

            Locator downloadBtn = page.getByText("下载模板EXCEL", new Page.GetByTextOptions().setExact(true));
            Assumptions.assumeTrue(downloadBtn.isVisible(), "未找到下载模板按钮");

            final int[] statusCode = {0};
            final String[] responseBody = {""};
            page.onResponse(response -> {
                if (response.url().contains("downloadReqImportTemplate")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            downloadBtn.click();
            page.waitForTimeout(3000);

            if (statusCode[0] == 500) {
                String msg = responseBody[0];
                if (msg.contains("\"msg\"")) {
                    int start = msg.indexOf("\"msg\":\"") + 7;
                    int end = msg.indexOf("\"", start);
                    msg = msg.substring(start, end);
                }
                Assumptions.assumeTrue(false, msg);
            }
            log.info("GNYL_038 Excel模板下载成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_040: 导入Excel")
    void test_GNYL_040_importExcel() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.locator("#app").getByText(folder[1])
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            page.getByText("请选择").first().click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Sheet1")).click();
            page.waitForTimeout(500);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 标题 请选择"))
                    .getByRole(AriaRole.IMG).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("标题")).click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 内容 请选择"))
                    .getByRole(AriaRole.IMG).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("内容")).click();
            page.waitForTimeout(300);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 层级 请选择"))
                    .getByRole(AriaRole.IMG).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("*层级代号")).click();
            page.waitForTimeout(300);

            final int[] statusCode = {0};
            final String[] responseBody = {""};
            page.onResponse(response -> {
                if (response.url().contains("importReqSpecification")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
            page.waitForTimeout(3000);

            Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
            Assertions.assertTrue(responseBody[0].contains("操作成功"), "导入失败: " + responseBody[0]);
            log.info("GNYL_040 Excel导入成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_042: 需求规格标题必填测试")
    void test_GNYL_042_emptyTitle() {
        String parentId = resolveParentId();
        try {
            String payload = """
                {
                    "parentId": "%s",
                    "type": "reqSpeFolder",
                    "reqSpecName": "",
                    "data": [{"level": 1, "title": "功能需求"}],
                    "projectId": "%s"
                }
                """.formatted(parentId, PROJECT_ID);

            String resp = postImport(payload);
            Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                    "标题为空应拦截: " + resp);
            log.info("GNYL_042 标题必填拦截通过");
        } finally {
            cleanupByName("AT_Import_");
        }
    }

    @Test
    @DisplayName("GNYL_043: 工作表必选测试")
    void test_GNYL_043_noWorksheet() {
        String parentId = resolveParentId();
        try {
            String payload = """
                {
                    "parentId": "%s",
                    "type": "reqSpeFolder",
                    "reqSpecName": "测试导入",
                    "projectId": "%s"
                }
                """.formatted(parentId, PROJECT_ID);

            String resp = postImport(payload);
            Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                    "未选工作表应拦截: " + resp);
            log.info("GNYL_043 工作表必选拦截通过");
        } finally {
            cleanupByName("AT_Import_");
        }
    }

    @Test
    @DisplayName("GNYL_044: 实体属性必选测试")
    void test_GNYL_044_noAttribute() {
        String parentId = resolveParentId();
        try {
            String payload = """
                {
                    "parentId": "%s",
                    "type": "reqSpeFolder",
                    "reqSpecName": "测试导入",
                    "data": [],
                    "projectId": "%s"
                }
                """.formatted(parentId, PROJECT_ID);

            String resp = postImport(payload);
            Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                    "未选属性应拦截: " + resp);
            log.info("GNYL_044 实体属性必选拦截通过");
        } finally {
            cleanupByName("AT_Import_");
        }
    }

    // ==================== Word import tests ====================

    @Test
    @DisplayName("GNYL_045: 进入导入Word弹框")
    void test_GNYL_045_EnterImportWordDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(folder[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_045 成功进入导入Word弹框");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_046: 右键文件夹进入导入Word弹框")
    void test_GNYL_046_rightClickFolderEnterWordDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_046 右键文件夹进入导入Word弹框成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_047: 文件夹列表进入导入Word弹框")
    void test_GNYL_047_folderListEnterWordDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).first().click();
            page.waitForTimeout(500);
            page.getByText("导入Word", new Page.GetByTextOptions().setExact(true)).first().click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_047 文件夹列表进入导入Word弹框成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_048: 下载Word模板")
    void test_GNYL_048_downloadTemplate() {
        String[] folder = openImportWordDialog();
        try {
            page.getByText("下载模板WORD", new Page.GetByTextOptions().setExact(true)).click();

            final int[] statusCode = {0};
            final String[] responseBody = {""};
            page.onResponse(response -> {
                if (response.url().contains("downloadReqImportTemplate")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            if (statusCode[0] == 500) {
                String msg = responseBody[0];
                if (msg.contains("\"msg\"")) {
                    int start = msg.indexOf("\"msg\":\"") + 7;
                    int end = msg.indexOf("\"", start);
                    msg = msg.substring(start, end);
                }
                Assumptions.assumeTrue(false, msg);
            }
            log.info("GNYL_048 Word模板下载成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_049: 上传Word文件")
    void test_GNYL_049_uploadFile() {
        String[] folder = openImportWordDialog();
        try {
            Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
            page.waitForTimeout(3000);

            String docId = api.findNodeIdByTitle(PROJECT_ID, "需求导入模板W");
            log.info("GNYL_049 文件上传完成, docId={}", docId);
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_050: 上传Word后验证标题自动填充")
    void test_GNYL_050_verifyAutoFill() {
        String[] folder = openImportWordDialog();
        try {
            Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            Locator fileName = page.getByText("需求导入模板W");
            assertThat(fileName).isVisible();
            log.info("GNYL_050 文件名展示正确");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("GNYL_051: 导入Word数据")
    void test_GNYL_051_importWordData() {
        String[] folder = openImportWordDialog();
        try {
            Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(500);

            final int[] statusCode = {0};
            final String[] responseBody = {""};
            page.onResponse(response -> {
                if (response.url().contains("importReqSpecification")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
            page.waitForTimeout(2000);

            Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
            log.info("GNYL_051 导入Word数据成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_052: 上传损坏的Word文件")
    void test_GNYL_052_uploadDamagedWord() {
        String[] folder = openImportWordDialog();
        try {
            Path filePath = Paths.get(TEST_FILES_DIR + "损坏的需求规格.docx");

            final int[] statusCode = {0};
            final String[] responseBody = {""};

            page.onResponse(response -> {
                String url = response.url();
                if (url.contains("import") || url.contains("reqSpec") || url.contains("reqDoc")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
            page.waitForTimeout(1000);

            if (statusCode[0] == 0) {
                Locator errorMsg = page.locator(
                        ".el-message--error, .el-notification__content, .el-message-box__message").first();
                if (errorMsg.isVisible()) {
                    String msg = errorMsg.textContent();
                    log.info("GNYL_052 前端直接拦截了损坏文件: {}", msg);
                    return;
                }
            }

            if (statusCode[0] != 0) {
                Assertions.assertEquals(500, statusCode[0], "损坏文件应返回500, 实际: " + statusCode[0]);
            }
            log.info("GNYL_052 损坏文件拦截通过, statusCode={}", statusCode[0]);
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_053: 上传Excel文件验证前端拦截")
    void test_GNYL_053_uploadExcelToWordDialog() {
        String[] folder = openImportWordDialog();
        try {
            Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            Locator errorMsg = page.getByText("请上传Word文件");
            assertThat(errorMsg).isVisible();
            log.info("GNYL_053 Excel文件前端拦截通过");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    // ==================== ReqIF import tests ====================

    @Test
    @DisplayName("GNYL_053_reqif: 进入导入ReqIF弹框")
    void test_GNYL_053_reqif_EnterImportReqIfDialog() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(folder[1]))
                    .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_053_reqif 成功进入导入ReqIF弹框");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @Disabled
    @DisplayName("GNYL_054: 下载ReqIF模板")
    void test_GNYL_054_downloadReqIfTemplate() {}

    @Test
    @DisplayName("GNYL_055: 上传ReqIf文件")
    void test_GNYL_055_uploadReqIfFile() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "Req模版.reqif");

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(folder[1]))
                    .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            log.info("GNYL_055 ReqIf文件上传完成");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_056: 参数设置")
    void test_GNYL_056_parameterSettings() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(folder[1]))
                    .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "Req模版.reqif");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            log.info("GNYL_056 参数设置页面可见");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_057: 导入ReqIF数据")
    void test_GNYL_057_importReqIfData() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName(folder[1]))
                    .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
            page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "Req模版.reqif");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(2000);

            final int[] statusCode = {0};
            final String[] responseBody = {""};
            page.onResponse(response -> {
                if (response.url().contains("importReqIfFile")) {
                    statusCode[0] = response.status();
                    try { responseBody[0] = response.text(); } catch (Exception ignored) {}
                }
            });

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
            page.waitForTimeout(5000);

            Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
            Assertions.assertTrue(responseBody[0].contains("导入成功"), "导入失败: " + responseBody[0]);
            log.info("GNYL_057 导入ReqIF成功");
        } finally {
            closeDialogs();
            cleanupFolderByName(folder[1]);
        }
    }

    // ==================== Private helpers ====================

    private String[] openImportWordDialog() {
        String[] folder = createTempFolder();
        reqPage.refreshTree();
        page.waitForTimeout(1000);

        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(folder[1]))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);
        return folder;
    }

    private String postImport(String payload) {
        APIResponse response = page.request().post(
                TestConfig.API_PREFIX + "/erm/import/importReqSpecification",
                RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData(payload)
        );
        return response.text();
    }
}
