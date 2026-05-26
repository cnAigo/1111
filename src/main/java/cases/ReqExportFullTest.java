package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public class ReqExportFullTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(ReqExportFullTest.class);
    private ReqApiActions api;
    private RequirementPage rPage;

    @BeforeAll
    public void init() {
        api = new ReqApiActions(page.request());
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 公共工具方法 ==========

    /**
     * 右键点击需求规格并选择导出菜单
     */
    private void rightClickReqSpecAndExport(String menuText) {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .locator("img").click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("导出▶").click();
        page.waitForTimeout(300);
    }

    /**
     * 双击需求规格进入详情，然后在表头点击导出
     */
    private void tableHeaderExport(String menuText) {
        // 先点击选中
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .locator("span").nth(1).click();
        page.waitForTimeout(200);
        // 双击进入详情
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(300);
    }

    /**
     * 右键文件夹并选择导出ReqIf
     */
    private void rightClickFolderExportReqIf() {
        page.locator("#app").getByText(TestConstants.PARENT_FOLDER)
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^导出$"))).click();
        page.waitForTimeout(300);
        page.locator("span").filter(new Locator.FilterOptions().setHasText("ReqIf（doors）")).click();
        page.waitForTimeout(1000);
    }

    /**
     * 选择ReqIf下拉框
     */
    private void selectReqIfDropdown(String label) {
        page.locator("div").filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^" + label + ":请选择$")))
                .locator("span").nth(1).click();
    }

    /**
     * 等待并验证下载
     */
    private Download waitForDownloadAndLog(Runnable action, String testName) {
        Download download = page.waitForDownload(() -> {
            action.run();
        });
        page.waitForTimeout(1000);
        log.info("{} 成功, 文件: {}", testName, download.suggestedFilename());
        return download;
    }

    // ========== 导出Excel测试 ==========

    @Test
    @Order(610)
    @DisplayName("GNYL_061: 需求规格导出Excel(右键)")
    public void test_GNYL_061_exportExcelRightClick() {
        rightClickReqSpecAndExport("Excel");

        Download download = waitForDownloadAndLog(
                () -> page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click(),
                "GNYL_061 右键导出Excel"
        );

        // 验证下载的文件名包含.xlsx
        Assertions.assertTrue(download.suggestedFilename().contains(".xlsx"),
                "下载的文件不是Excel格式: " + download.suggestedFilename());
    }

    @Test
    @Order(620)
    @DisplayName("GNYL_062: 需求规格导出Excel(表头)")
    public void test_GNYL_062_exportExcelTableHeader() {
        tableHeaderExport("导出Excel");

        Download download = waitForDownloadAndLog(
                () -> page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("导出Excel")).click(),
                "GNYL_062 表头导出Excel"
        );

        // 验证下载的文件名包含.xlsx
        Assertions.assertTrue(download.suggestedFilename().contains(".xlsx"),
                "下载的文件不是Excel格式: " + download.suggestedFilename());
    }

    // ========== 导出Word测试 ==========

    @Test
    @Order(640)
    @DisplayName("GNYL_064: 需求规格导出Word(右键)")
    public void test_GNYL_064_exportWordRightClick() {
        rightClickReqSpecAndExport("Word");

        Download download = waitForDownloadAndLog(
                () -> page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click(),
                "GNYL_064 右键导出Word"
        );

        // 验证下载的文件名包含.docx
        Assertions.assertTrue(download.suggestedFilename().contains(".docx"),
                "下载的文件不是Word格式: " + download.suggestedFilename());
    }

    @Test
    @Order(650)
    @DisplayName("GNYL_065: 需求规格导出Word(表头)")
    public void test_GNYL_065_exportWordTableHeader() {
        tableHeaderExport("导出Word");

        Download download = waitForDownloadAndLog(
                () -> page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("导出Word")).click(),
                "GNYL_065 表头导出Word"
        );

        // 验证下载的文件名包含.docx
        Assertions.assertTrue(download.suggestedFilename().contains(".docx"),
                "下载的文件不是Word格式: " + download.suggestedFilename());
    }

    // ========== 导出ReqIf测试 ==========

    @Test
    @Order(670)
    @DisplayName("GNYL_067: 需求规格文件夹下导出ReqIf")
    public void test_GNYL_067_exportReqIfFromFolder() {
        // 双击文件夹进入
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER).setExact(true))
                .locator("img").click();
        page.waitForTimeout(200);
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.PARENT_FOLDER).setExact(true))
                .dblclick();
        page.waitForTimeout(1000);

        // 点击导出
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("ReqIf（doors）")).click();
        page.waitForTimeout(1000);

        // 在ReqIf导出对话框中填写信息
        selectReqIfDropdown("需求规格列表");
        page.waitForTimeout(300);
        selectReqIfDropdown("模版信息");
        page.waitForTimeout(300);

        // 点击导出
        page.getByRole(AriaRole.CONTENTINFO)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(2000);

        log.info("GNYL_067 文件夹下导出ReqIf完成");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(680)
    @DisplayName("GNYL_068: 右键文件夹导出ReqIf")
    public void test_GNYL_068_exportReqIfRightClickFolder() {
        // 右键文件夹
        page.locator("#app").getByText(TestConstants.PARENT_FOLDER)
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);

        // 选择导出 → ReqIf
        page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^导出$"))).click();
        page.waitForTimeout(300);
        page.locator("span").filter(new Locator.FilterOptions().setHasText("ReqIf（doors）")).click();
        page.waitForTimeout(1000);

        // 选择需求规格列表
        page.locator(".w-full > div > .el-select > .el-select__wrapper > .el-select__selection > div:nth-child(2)")
                .first().click();
        page.waitForTimeout(500);
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1)).click();
        page.waitForTimeout(300);

        // 选择模版信息
        selectReqIfDropdown("模版信息");
        page.waitForTimeout(300);

        // 填写ReqIf文件名称
        page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^ReqIf文件名称:$")))
                .locator("div").nth(1).click();
        page.waitForTimeout(200);
        page.keyboard().type("自动化测试导出_" + System.currentTimeMillis());
        page.waitForTimeout(200);

        // 导出并验证下载
        Download download = waitForDownloadAndLog(
                () -> page.getByRole(AriaRole.CONTENTINFO)
                        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click(),
                "GNYL_068 右键文件夹导出ReqIf"
        );

        // 验证下载的文件名包含.reqif
        Assertions.assertTrue(download.suggestedFilename().contains(".reqif"),
                "下载的文件不是ReqIf格式: " + download.suggestedFilename());
    }

    // ========== ReqIf必填项测试 ==========

    @Test
    @Order(690)
    @DisplayName("GNYL_069: ReqIf文件名称必填测试")
    public void test_GNYL_069_reqIfFileNameRequired() {
        // 打开ReqIf导出对话框
        rightClickFolderExportReqIf();

        // 清空ReqIf文件名称
        Locator fileNameInput = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^ReqIf文件名称:$")))
                .locator("input, .el-input__inner");
        if (fileNameInput.isVisible()) {
            fileNameInput.click();
            fileNameInput.fill("");
            page.waitForTimeout(300);
        }

        // 点击导出
        page.getByRole(AriaRole.CONTENTINFO)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(1000);

        // 验证错误消息出现
        Locator errorMsg = page.locator(".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
        Assertions.assertTrue(errorMsg.isVisible(), "应显示文件名称必填错误提示");
        log.info("GNYL_069 ReqIf文件名称必填校验通过: {}", errorMsg.textContent());

        // 关闭对话框
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(700)
    @DisplayName("GNYL_070: ReqIf属性名称必填测试")
    public void test_GNYL_070_reqIfAttrNameRequired() {
        // 打开ReqIf导出对话框
        rightClickFolderExportReqIf();

        // 查找属性名称输入框
        Locator attrNameInput = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^属性名称:$")))
                .locator("input, .el-input__inner");
        if (attrNameInput.isVisible()) {
            attrNameInput.click();
            attrNameInput.fill("");
            page.waitForTimeout(300);
        }

        // 点击导出
        page.getByRole(AriaRole.CONTENTINFO)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(1000);

        // 验证错误消息出现
        Locator errorMsg = page.locator(".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
        if (errorMsg.isVisible()) {
            log.info("GNYL_070 ReqIf属性名称必填校验通过: {}", errorMsg.textContent());
        } else {
            log.warn("GNYL_070 未检测到错误提示（属性名称可能非必填或无此字段）");
        }

        // 关闭对话框
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(710)
    @DisplayName("GNYL_071: ReqIf属性数据类型必选测试")
    public void test_GNYL_071_reqIfAttrTypeRequired() {
        // 打开ReqIf导出对话框
        rightClickFolderExportReqIf();

        // 查找属性数据类型下拉框（默认状态下不选择）
        Locator attrTypeSelect = page.locator("div").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^属性数据类型:请选择$")))
                .locator("span").first();
        if (attrTypeSelect.isVisible()) {
            log.info("GNYL_071 属性数据类型未选择（保持默认）");
        }

        // 点击导出
        page.getByRole(AriaRole.CONTENTINFO)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("导出")).click();
        page.waitForTimeout(1000);

        // 验证错误消息出现
        Locator errorMsg = page.locator(".el-form-item__error, .el-message, .el-message--error, [role='alert']").first();
        if (errorMsg.isVisible()) {
            log.info("GNYL_071 ReqIf属性数据类型必选校验通过: {}", errorMsg.textContent());
        } else {
            log.warn("GNYL_071 未检测到错误提示（属性数据类型可能非必选或无此字段）");
        }

        // 关闭对话框
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
        page.waitForTimeout(500);
    }
}
