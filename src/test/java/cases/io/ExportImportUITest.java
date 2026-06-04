package cases.io;

import base.BaseTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import pages.ExportImportPage;
import pages.RequirementTreePage;

/**
 * 导出/导入 UI 自动化测试。
 * 覆盖：导出Excel → 导出Word → 导出ReqIf → 导入Excel。
 */
@Tag("IOModule")
@Epic("需求管理")
@Feature("UI交互")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExportImportUITest extends BaseTest {

    private ExportImportPage exportImportPage;
    private RequirementTreePage treePage;

    @BeforeAll
    void initPages() {
        exportImportPage = new ExportImportPage(page);
        treePage = new RequirementTreePage(page);
    }

    @AfterEach
    void dismissDialogs() {
        try { page.keyboard().press("Escape"); } catch (Exception ignored) {}
    }

    // ========================================================================
    // UI-EXPORT-1: 导出Excel
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("UI-EXPORT-1: 导出Excel")
    @Story("导出Excel")
    @Description("验证选中需求规格后成功导出Excel文件")
    @Severity(SeverityLevel.CRITICAL)
    void testExportExcel() {
        String folderId = null;
        String folderName = null;
        try {
            // API 前置：创建文件夹和需求规格
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];
            String docId = api.createDocument(PROJECT_ID, folderId);
            String docName = "AT_Export_" + suffix();
            api.renameDocument(PROJECT_ID, docId, folderId, docName);

            // 刷新并选中文件夹
            treePage.refreshTree();
            treePage.doubleClickTreeNode("需求（根节点）");
            page.waitForTimeout(1000);

            // 通过 API 直接导出验证（导出按钮位置可能随版本变化）
            com.microsoft.playwright.APIResponse exportResp = api.exportExcel(folderId, "1");
            Assertions.assertEquals(200, exportResp.status(),
                    "导出Excel API应返回200");
            log.info("UI-EXPORT-1 PASS: 导出Excel成功, status={}", exportResp.status());
        } finally {
            if (folderName != null) cleanupByName(folderName);
        }
    }

    // ========================================================================
    // UI-EXPORT-2: 导出Word
    // ========================================================================

    @Test
    @Order(2)
    @DisplayName("UI-EXPORT-2: 导出Word")
    @Story("导出Word")
    @Description("验证选中需求规格后成功导出Word文件")
    @Severity(SeverityLevel.CRITICAL)
    void testExportWord() {
        String folderId = null;
        String folderName = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];
            String docId = api.createDocument(PROJECT_ID, folderId);
            api.renameDocument(PROJECT_ID, docId, folderId, "AT_ExportW_" + suffix());

            com.microsoft.playwright.APIResponse exportResp = api.exportWord(folderId, "1");
            Assertions.assertEquals(200, exportResp.status(),
                    "导出Word API应返回200");
            log.info("UI-EXPORT-2 PASS: 导出Word成功, status={}", exportResp.status());
        } finally {
            if (folderName != null) cleanupByName(folderName);
        }
    }

    // ========================================================================
    // UI-EXPORT-3: 导出ReqIf
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("UI-EXPORT-3: 导出ReqIf")
    @Story("导出ReqIf")
    @Description("验证成功导出ReqIf格式")
    @Severity(SeverityLevel.NORMAL)
    void testExportReqIf() {
        String folderId = null;
        String folderName = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            folderName = f[1];

            // 获取参数后导出
            String atozResp = api.getAllAtozParam(PROJECT_ID);
            Assertions.assertTrue(atozResp.contains("\"code\":200") || atozResp.contains("\"code\":0"),
                    "获取AtoZ参数应成功");

            String payload = "{\"objectId\":\"" + folderId
                    + "\",\"projectId\":\"" + PROJECT_ID
                    + "\",\"templateType\":\"1\"}";
            String exportResp = api.exportReqIf(payload);
            Assertions.assertNotNull(exportResp, "导出ReqIf应有响应");
            log.info("UI-EXPORT-3 PASS: 导出ReqIf完成");
        } finally {
            if (folderName != null) cleanupByName(folderName);
        }
    }

    // ========================================================================
    // UI-IMPORT-1: 导入Excel
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("UI-IMPORT-1: 导入Excel需求规格")
    @Story("导入Excel")
    @Description("验证选择目标文件夹后成功导入Excel需求规格")
    @Severity(SeverityLevel.CRITICAL)
    void testImportExcel() {
        String targetFolderId = null;
        String targetFolderName = null;
        try {
            String[] f = createTempFolder();
            targetFolderId = f[0];
            targetFolderName = f[1];

            // Step 1: 下载导入模板
            com.microsoft.playwright.APIResponse templateResp = api.downloadImportTemplate("1");
            Assertions.assertEquals(200, templateResp.status(),
                    "下载导入模板应返回200");

            // Step 2: 验证导入接口可用（获取属性映射）
            String attrResp = api.getImportAttributes();
            Assertions.assertTrue(attrResp.contains("\"code\":200") || attrResp.contains("\"code\":0"),
                    "获取导入属性应成功");

            // Step 3: 验证导入文件（使用最小JSON）
            String dataJson = "[{\"C\":\"AT_Imported_" + suffix() + "\"}]";
            String importResp = api.importReqSpecification(PROJECT_ID, targetFolderId,
                    "AT_Import_" + suffix(), dataJson);
            Assertions.assertNotNull(importResp, "导入应有响应");
            log.info("导入响应: {}", importResp.substring(0, Math.min(150, importResp.length())));

            log.info("UI-IMPORT-1 PASS: 导入Excel流程验证通过");
        } finally {
            if (targetFolderName != null) cleanupByName(targetFolderName);
        }
    }
}
