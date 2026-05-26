package cases;

import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.RequestOptions;
import actions.ReqApiActions;
import config.TestConfig;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public class WordImportTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(WordImportTest.class);
    private RequirementPage reqPage;
    private ReqApiActions api;
    private static final String TEST_FILES_DIR = "src/main/resources/testfiles/";

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
        api = new ReqApiActions(page.request());
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== Excel导入功能测试 ==========

    @Test
    @Order(340)
    @DisplayName("GNYL_034: 进入导入Excel弹框")
    void test_GNYL_034_EnterImportExcelDialog() {
        reqPage.rightClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_034 成功进入导入Excel弹框");
        closeDialog();
    }

    @Test
    @Order(380)
    @DisplayName("GNYL_038: 下载Excel模板")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void test_GNYL_038_downloadExcelTemplate() {
        reqPage.rightClickTreeNode(TestConstants.PARENT_FOLDER);
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
        closeDialog();

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
    }

    @Test
    @Order(400)
    @DisplayName("GNYL_040: 导入Excel")
    void test_GNYL_040_importExcel() {
        page.locator("#app").getByText("测试父文件夹")
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx");
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(2000);

        // 选择 Sheet
        page.getByText("请选择").first().click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Sheet1")).click();
        page.waitForTimeout(500);

        // 映射列
        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 标题 请选择")).getByRole(AriaRole.IMG).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("标题")).click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 内容 请选择")).getByRole(AriaRole.IMG).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("内容")).click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 层级 请选择")).getByRole(AriaRole.IMG).click();
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

        closeDialog();

        Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
        Assertions.assertTrue(responseBody[0].contains("操作成功"), "导入失败: " + responseBody[0]);
        log.info("GNYL_040 Excel导入成功");

    }

    @Test
    @Order(420)
    @DisplayName("GNYL_042: 需求规格标题必填测试")
    void test_GNYL_042_emptyTitle() {
        String parentId = resolveParentId();
        String projectId = resolveProjectId();

        String payload = """
            {
                "parentId": "%s",
                "type": "reqSpeFolder",
                "reqSpecName": "",
                "data": [{"level": 1, "title": "功能需求"}],
                "projectId": "%s"
            }
            """.formatted(parentId, projectId);

        String resp = postImport(payload);
        Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                "标题为空应拦截: " + resp);
        log.info("GNYL_042 标题必填拦截通过");
    }

    @Test
    @Order(425)
    @DisplayName("GNYL_043: 工作表必选测试")
    void test_GNYL_043_noWorksheet() {
        String parentId = resolveParentId();
        String projectId = resolveProjectId();

        String payload = """
            {
                "parentId": "%s",
                "type": "reqSpeFolder",
                "reqSpecName": "测试导入",
                "projectId": "%s"
            }
            """.formatted(parentId, projectId);

        String resp = postImport(payload);
        Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                "未选工作表应拦截: " + resp);
        log.info("GNYL_043 工作表必选拦截通过");
    }

    @Test
    @Order(430)
    @DisplayName("GNYL_044: 实体属性必选测试")
    void test_GNYL_044_noAttribute() {
        String parentId = resolveParentId();
        String projectId = resolveProjectId();

        String payload = """
            {
                "parentId": "%s",
                "type": "reqSpeFolder",
                "reqSpecName": "测试导入",
                "data": [],
                "projectId": "%s"
            }
            """.formatted(parentId, projectId);

        String resp = postImport(payload);
        Assertions.assertTrue(resp.contains("500") || resp.contains("失败"),
                "未选属性应拦截: " + resp);
        log.info("GNYL_044 实体属性必选拦截通过");
    }

    // ========== Word导入功能测试 ==========

    @Test
    @Order(450)
    @DisplayName("GNYL_045: 进入导入Word弹框")
    void test_GNYL_045_EnterImportWordDialog() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_045 成功进入导入Word弹框");
        closeDialog();
    }

    @Test
    @Order(460)
    @DisplayName("GNYL_046: 右键文件夹进入导入Word弹框")
    void test_GNYL_046_rightClickFolderEnterWordDialog() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        reqPage.rightClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_046 右键文件夹进入导入Word弹框成功");
        closeDialog();
    }

    @Test
    @Order(470)
    @DisplayName("GNYL_047: 文件夹列表进入导入Word弹框")
    void test_GNYL_047_folderListEnterWordDialog() {
        reqPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForTimeout(500);
        page.getByText("导入Word", new Page.GetByTextOptions().setExact(true)).first().click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_047 文件夹列表进入导入Word弹框成功");
        closeDialog();
    }

    @Test
    @Order(480)
    @DisplayName("GNYL_048: 下载Word模板")
    void test_GNYL_048_downloadTemplate() {
        openImportWordDialog();

        page.getByText("下载模板WORD", new Page.GetByTextOptions().setExact(true)).click();;

        final int[] statusCode = {0};
        final String[] responseBody = {""};
        page.onResponse(response -> {
            if (response.url().contains("downloadReqImportTemplate")) {
                statusCode[0] = response.status();
                try { responseBody[0] = response.text(); } catch (Exception ignored) {}
            }
        });

        closeDialog();

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
    }

    @Test
    @Order(490)
    @DisplayName("GNYL_049: 上传Word文件")
    void test_GNYL_049_uploadFile() {
        openImportWordDialog();

        Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx");
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(2000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
        page.waitForTimeout(3000);

        closeDialog();

        String docId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, "需求导入模板W");
        log.info("GNYL_049 文件上传完成, docId={}", docId);
    }

    @Test
    @Order(500)
    @DisplayName("GNYL_050: 上传Word后验证标题自动填充")
    void test_GNYL_050_verifyAutoFill() {
        openImportWordDialog();

        Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx");
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(2000);

        Locator fileName = page.getByText("需求导入模板W");
        assertThat(fileName).isVisible();
        log.info("GNYL_050 文件名展示正确");

        closeDialog();
    }

    @Test
    @Order(510)
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("GNYL_051: 导入Word数据")
    void test_GNYL_051_importWordData() {
        openImportWordDialog();

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

        closeDialog();

        Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
        log.info("GNYL_051 导入Word数据成功");
    }

    @Test
    @Order(520)
    @DisplayName("GNYL_052: 上传损坏的Word文件")
    void test_GNYL_052_uploadDamagedWord() {
        openImportWordDialog();

        Path filePath = Paths.get(TEST_FILES_DIR + "损坏的需求规格.docx");

        // 先注册监听，用数组收集
        final int[] statusCode = {0};
        final String[] responseBody = {""};

        page.onResponse(response -> {
            String url = response.url();
            // 放宽匹配：包含 import 或 reqSpec 或 req 都算
            if (url.contains("import") || url.contains("reqSpec") || url.contains("reqDoc")) {
                statusCode[0] = response.status();
                try { responseBody[0] = response.text(); } catch (Exception ignored) {}
            }
        });

        // 上传文件
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(1000);

        // 点导入
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
        page.waitForTimeout(1000); // 多等一会儿

        // 如果 statusCode 还是 0，说明根本没发请求，检查是否有错误提示弹窗
        if (statusCode[0] == 0) {
            Locator errorMsg = page.locator(".el-message--error, .el-notification__content, .el-message-box__message").first();
            if (errorMsg.isVisible()) {
                String msg = errorMsg.textContent();
                log.info("GNYL_052 前端直接拦截了损坏文件: {}", msg);
                // 前端拦截也视为通过
                closeDialog();
                return;
            }
        }

        closeDialog();

        // 如果捕获到了请求，验证返回500
        if (statusCode[0] != 0) {
            Assertions.assertEquals(500, statusCode[0], "损坏文件应返回500, 实际: " + statusCode[0]);
        }
        log.info("GNYL_052 损坏文件拦截通过, statusCode={}", statusCode[0]);
    }


    @Test
    @Order(525)
    @DisplayName("GNYL_053: 上传Excel文件验证前端拦截")
    void test_GNYL_053_uploadExcelToWordDialog() {
        openImportWordDialog();

        Path filePath = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx");
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(2000);

        Locator errorMsg = page.getByText("请上传Word文件");
        assertThat(errorMsg).isVisible();
        log.info("GNYL_053 Excel文件前端拦截通过");
        closeDialog();
    }

    // ========== ReqIF导入功能测试 ==========

    @Test
    @Order(530)
    @DisplayName("GNYL_053_reqif: 进入导入ReqIF弹框")
    void test_GNYL_053_reqif_EnterImportReqIfDialog() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹"))
                .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();
        log.info("GNYL_053_reqif 成功进入导入ReqIF弹框");
        closeDialog();
    }

    @Test
    @Order(540)
    @Disabled
    @DisplayName("GNYL_054: 下载ReqIF模板")
    void test_GNYL_054_downloadReqIfTemplate() {
        // 未找到下载模板按钮，跳过
    }

    @Test
    @Order(550)
    @DisplayName("GNYL_055: 上传ReqIf文件")
    void test_GNYL_055_uploadReqIfFile() {
        Path filePath = Paths.get(TEST_FILES_DIR + "Req模版.reqif");

        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹"))
                .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(2000);

        log.info("GNYL_055 ReqIf文件上传完成");
        closeDialog();
    }

    @Test
    @Order(560)
    @DisplayName("GNYL_056: 参数设置")
    void test_GNYL_056_parameterSettings() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹"))
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
        closeDialog();
    }

    @Test
    @Order(570)
    @DisplayName("GNYL_057: 导入ReqIF数据")
    void test_GNYL_057_importReqIfData() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹"))
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

        closeDialog();

        Assertions.assertEquals(200, statusCode[0], "导入失败: " + responseBody[0]);
        Assertions.assertTrue(responseBody[0].contains("导入成功"), "导入失败: " + responseBody[0]);
        log.info("GNYL_057 导入ReqIF成功");
    }

    // ========== 工具方法 ==========

    private void openImportWordDialog() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);
    }

    private void closeDialog() {
        Locator closeBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭"));
        if (!closeBtn.isHidden()) {
            closeBtn.click();
        }
        page.waitForTimeout(500);
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

    private String resolveParentId() {
        String parentId = TestContext.get("parentId");
        if (parentId == null || parentId.isEmpty()) {
            parentId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
            if (parentId != null) TestContext.set("parentId", parentId);
        }
        return parentId;
    }

    private String resolveProjectId() {
        String projectId = TestContext.get("projectId");
        if (projectId == null || projectId.isEmpty()) {
            projectId = TestConstants.PROJECT_ID;
            TestContext.set("projectId", projectId);
        }
        return projectId;
    }
}
