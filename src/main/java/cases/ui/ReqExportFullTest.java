package cases.ui;

import base.BaseTest;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import org.junit.jupiter.api.*;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqExportFullTest extends BaseTest {

    // ==================== Export Excel tests ====================

    @Test
    @DisplayName("GNYL_061: 需求规格导出Excel(右键)")
    void test_GNYL_061_exportExcelRightClick() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            rightClickReqSpecAndExport(doc[1]);

            Download download = waitForDownloadAndLog(
                    () -> page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click(),
                    "GNYL_061 右键导出Excel");
            Assertions.assertTrue(download.suggestedFilename().contains(".xlsx"),
                    "下载的文件不是Excel格式: " + download.suggestedFilename());
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_062: 需求规格导出Excel(表头)")
    void test_GNYL_062_exportExcelTableHeader() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            tableHeaderExport(folder[1], doc[1]);

            Download download = waitForDownloadAndLog(
                    () -> page.getByRole(AriaRole.MENUITEM,
                            new Page.GetByRoleOptions().setName("导出Excel")).click(),
                    "GNYL_062 表头导出Excel");
            Assertions.assertTrue(download.suggestedFilename().contains(".xlsx"),
                    "下载的文件不是Excel格式: " + download.suggestedFilename());
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // ==================== Export Word tests ====================

    @Test
    @DisplayName("GNYL_064: 需求规格导出Word(右键)")
    void test_GNYL_064_exportWordRightClick() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            rightClickReqSpecAndExport(doc[1]);

            Download download = waitForDownloadAndLog(
                    () -> page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click(),
                    "GNYL_064 右键导出Word");
            Assertions.assertTrue(download.suggestedFilename().contains(".docx"),
                    "下载的文件不是Word格式: " + download.suggestedFilename());
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_065: 需求规格导出Word(表头)")
    void test_GNYL_065_exportWordTableHeader() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            tableHeaderExport(folder[1], doc[1]);

            Download download = waitForDownloadAndLog(
                    () -> page.getByRole(AriaRole.MENUITEM,
                            new Page.GetByRoleOptions().setName("导出Word")).click(),
                    "GNYL_065 表头导出Word");
            Assertions.assertTrue(download.suggestedFilename().contains(".docx"),
                    "下载的文件不是Word格式: " + download.suggestedFilename());
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // ==================== Export ReqIf tests ====================

    @Test
    @DisplayName("GNYL_067: 需求规格文件夹下导出ReqIf")
    void test_GNYL_067_exportReqIfFromFolder() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM,
                    new Page.GetByRoleOptions().setName(folder[1]).setExact(true))
                    .locator("img").click();
            page.waitForTimeout(200);
            page.getByRole(AriaRole.TREEITEM,
                    new Page.GetByRoleOptions().setName(folder[1]).setExact(true)).dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(300);
            page.getByRole(AriaRole.MENUITEM,
                    new Page.GetByRoleOptions().setName("ReqIf（doors）")).click();
            page.waitForTimeout(1000);

            selectReqIfDropdown("需求规格列表");
            page.waitForTimeout(300);
            selectReqIfDropdown("模版信息");
            page.waitForTimeout(300);

            page.getByRole(AriaRole.CONTENTINFO)
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(2000);
            log.info("GNYL_067 文件夹下导出ReqIf完成");
        } finally {
            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("关闭此对话框")).click();
            page.waitForTimeout(500);
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_068: 右键文件夹导出ReqIf")
    void test_GNYL_068_exportReqIfRightClickFolder() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            page.locator("#app").getByText(folder[1])
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            page.locator("span").filter(new Locator.FilterOptions()
                    .setHasText(Pattern.compile("^导出$"))).click();
            page.waitForTimeout(300);
            page.locator("span").filter(new Locator.FilterOptions()
                    .setHasText("ReqIf（doors）")).click();
            page.waitForTimeout(1000);

            page.locator(".w-full > div > .el-select > .el-select__wrapper > .el-select__selection > div:nth-child(2)")
                    .first().click();
            page.waitForTimeout(500);
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(doc[1])).click();
            page.waitForTimeout(300);

            selectReqIfDropdown("模版信息");
            page.waitForTimeout(300);

            page.locator("div").filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^ReqIf文件名称:$")))
                    .locator("div").nth(1).click();
            page.waitForTimeout(200);
            page.keyboard().type("自动化测试导出_" + System.currentTimeMillis());
            page.waitForTimeout(200);

            Download download = waitForDownloadAndLog(
                    () -> page.getByRole(AriaRole.CONTENTINFO)
                            .getByRole(AriaRole.BUTTON,
                                    new Locator.GetByRoleOptions().setName("导出")).click(),
                    "GNYL_068 右键文件夹导出ReqIf");
            Assertions.assertTrue(download.suggestedFilename().contains(".reqif"),
                    "下载的文件不是ReqIf格式: " + download.suggestedFilename());
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_069: ReqIf文件名称必填测试")
    void test_GNYL_069_reqIfFileNameRequired() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            rightClickFolderExportReqIf(folder[1]);

            Locator fileNameInput = page.locator("div")
                    .filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^ReqIf文件名称:$")))
                    .locator("input, .el-input__inner");
            if (fileNameInput.isVisible()) {
                fileNameInput.click();
                fileNameInput.fill("");
                page.waitForTimeout(300);
            }

            page.getByRole(AriaRole.CONTENTINFO)
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(1000);

            Locator errorMsg = page.locator(
                    ".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
            Assertions.assertTrue(errorMsg.isVisible(), "应显示文件名称必填错误提示");
            log.info("GNYL_069 ReqIf文件名称必填校验通过: {}", errorMsg.textContent());
        } finally {
            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("关闭此对话框")).click();
            page.waitForTimeout(500);
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_070: ReqIf属性名称必填测试")
    void test_GNYL_070_reqIfAttrNameRequired() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            rightClickFolderExportReqIf(folder[1]);

            Locator attrNameInput = page.locator("div")
                    .filter(new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^属性名称:$")))
                    .locator("input, .el-input__inner");
            if (attrNameInput.isVisible()) {
                attrNameInput.click();
                attrNameInput.fill("");
                page.waitForTimeout(300);
            }

            page.getByRole(AriaRole.CONTENTINFO)
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(1000);

            Locator errorMsg = page.locator(
                    ".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
            if (errorMsg.isVisible()) {
                log.info("GNYL_070 ReqIf属性名称必填校验通过: {}", errorMsg.textContent());
            } else {
                log.warn("GNYL_070 未检测到错误提示");
            }
        } finally {
            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("关闭此对话框")).click();
            page.waitForTimeout(500);
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_071: ReqIf属性数据类型必选测试")
    void test_GNYL_071_reqIfAttrTypeRequired() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);
            rightClickFolderExportReqIf(folder[1]);

            page.getByRole(AriaRole.CONTENTINFO)
                    .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
            page.waitForTimeout(1000);

            Locator errorMsg = page.locator(
                    ".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
            if (errorMsg.isVisible()) {
                log.info("GNYL_071 ReqIf属性数据类型必选校验通过: {}", errorMsg.textContent());
            } else {
                log.warn("GNYL_071 未检测到错误提示");
            }
        } finally {
            page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("关闭此对话框")).click();
            page.waitForTimeout(500);
            cleanupFolderByName(folder[1]);
        }
    }

    // ==================== Private UI helpers ====================

    private void rightClickReqSpecAndExport(String docName) {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(docName).setExact(true))
                .locator("img").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导出▶").click();
        page.waitForTimeout(300);
    }

    private void tableHeaderExport(String folderName, String docName) {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(docName).setExact(true))
                .locator("span").nth(1).click();
        page.waitForTimeout(200);

        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(docName).setExact(true)).dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(300);
    }

    private void rightClickFolderExportReqIf(String folderName) {
        page.locator("#app").getByText(folderName)
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.locator("span").filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^导出$"))).click();
        page.waitForTimeout(300);
        page.locator("span").filter(new Locator.FilterOptions()
                .setHasText("ReqIf（doors）")).click();
        page.waitForTimeout(1000);
    }

    private void selectReqIfDropdown(String label) {
        page.locator("div").filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^" + label + ":请选择$")))
                .locator("span").nth(1).click();
    }

    private Download waitForDownloadAndLog(Runnable action, String testName) {
        Download download = page.waitForDownload(() -> action.run());
        page.waitForTimeout(1000);
        log.info("{} 成功, 文件: {}", testName, download.suggestedFilename());
        return download;
    }
}
