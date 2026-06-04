package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Page Object for 导出/导入 (Export/Import) operations within RequirementManagement.
 */
public class ExportImportPage {

    private final Page page;

    public ExportImportPage(Page page) {
        this.page = page;
    }

    // ==================== Export ====================

    @Step("点击导出Excel按钮")
    public void clickExportExcel() {
        clickExportOption("导出Excel", "Excel");
    }

    @Step("点击导出Word按钮")
    public void clickExportWord() {
        clickExportOption("导出Word", "Word");
    }

    @Step("点击导出ReqIf按钮")
    public void clickExportReqIf() {
        clickExportOption("导出ReqIf", "ReqIf");
    }

    private void clickExportOption(String exactLabel, String fallbackLabel) {
        try {
            Locator btn = page.locator("button, span, div")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile(exactLabel + "|导出")))
                    .first();
            if (btn.isVisible()) {
                btn.click();
                page.waitForTimeout(300);
                Locator option = page.locator("span, li")
                        .filter(new Locator.FilterOptions().setHasText(Pattern.compile(exactLabel + "|" + fallbackLabel)))
                        .first();
                if (option.isVisible()) option.click();
                page.waitForTimeout(1000);
            }
        } catch (TimeoutError ignored) {}
    }

    @Step("触发导出并等待下载")
    public Path waitForDownload(Runnable trigger) {
        try {
            com.microsoft.playwright.Download download = page.waitForDownload(trigger);
            Path savedPath = download.path();
            return savedPath;
        } catch (TimeoutError e) {
            trigger.run();
            page.waitForTimeout(2000);
            return null;
        }
    }

    // ==================== Import ====================

    @Step("点击导入按钮")
    public void clickImport() {
        Locator importBtn = page.locator("button, span")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("导入|import")))
                .first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(800);
        }
    }

    @Step("选择导入文件: {filePath}")
    public void selectImportFile(String filePath) {
        Locator fileInput = page.locator("input[type='file']").first();
        fileInput.setInputFiles(Paths.get(filePath));
        page.waitForTimeout(500);
    }

    @Step("点击确认导入按钮")
    public void clickConfirmImport() {
        page.locator(".el-dialog__footer button, .el-dialog:visible button, .el-message-box__btns button")
                .filter(new Locator.FilterOptions().setHasText(Pattern.compile("确|导入|确认"))).first().click();
        page.waitForTimeout(1500);
    }

    @Step("选择导入目标文件夹: {folderName}")
    public void selectTargetFolder(String folderName) {
        page.getByRole(AriaRole.TREEITEM,
                new Page.GetByRoleOptions().setName(folderName)).first().click();
        page.waitForTimeout(500);
    }

    // ==================== Toast ====================

    @Step("等待操作完成Toast")
    public String waitForToast() {
        try {
            Locator toast = page.locator(".el-message--success, .el-message--error, .el-message__content").first();
            toast.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
            return toast.textContent();
        } catch (TimeoutError e) {
            return "";
        }
    }

    @Step("获取成功/错误消息")
    public String getMessage() {
        try {
            Locator msg = page.locator(".el-message, .el-notification, .el-alert").first();
            if (msg.isVisible()) return msg.textContent();
        } catch (Exception ignored) {}
        return "";
    }
}
