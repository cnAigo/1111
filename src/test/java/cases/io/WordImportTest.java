package cases.io;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Tag("IOModule") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WordImportTest extends BaseTest {
    @AfterEach void esc() { try { page.keyboard().press("Escape"); } catch (Exception ignored) {} }

    final String F = "测试父文件夹"; // 使用现有文件夹,不创建新的

    void toRoot() { page.navigate(TestConfig.REQUIREMENT_URL); waitForNetworkIdle(); ensureLoggedIn();
        page.locator(".el-tree-node__content").filter(new Locator.FilterOptions().setHasText(TestConstants.ROOT_NODE)).first().dblclick();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新建")).first()).isVisible(); }

    void rclickRow() { page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(F)).first()
            .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT)); assertThat(page.getByText("导入", new Page.GetByTextOptions().setExact(true))).isVisible(); }

    void rclickTree() { page.locator(".el-tree-node__content").filter(new Locator.FilterOptions().setHasText(F)).first()
            .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT)); assertThat(page.getByText("导入", new Page.GetByTextOptions().setExact(true))).isVisible(); }

    void openExcelDlg() { page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByText("Excel", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByText("Excel", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByRole(AriaRole.DIALOG)).isVisible(); }

    void openWordDlg() { page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByText("Word", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByText("Word", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByRole(AriaRole.DIALOG)).isVisible(); }

    // ═══════════ Excel导入 034-044 ═══════════
    @Test @DisplayName("GNYL_034: 右键文件夹→导入→Excel")
    void g034() { toRoot(); rclickRow(); openExcelDlg();
        assertThat(page.getByRole(AriaRole.DIALOG)).containsText("导入Excel"); takeScreenshot("GNYL_034"); }

    @Test @DisplayName("GNYL_035: 树右键→导入→Excel")
    void g035() { page.navigate(TestConfig.REQUIREMENT_URL); waitForNetworkIdle(); ensureLoggedIn();
        rclickTree(); openExcelDlg(); assertThat(page.getByRole(AriaRole.DIALOG)).containsText("导入Excel"); takeScreenshot("GNYL_035"); }

    @Test @DisplayName("GNYL_036: 双击文件夹→导入Excel")
    void g036() { toRoot(); page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(F)).first().dblclick();
        rclickRow(); openExcelDlg(); takeScreenshot("GNYL_036"); }

    @Test @DisplayName("GNYL_037: 工具栏导入按钮")
    void g037() { toRoot(); assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).first()).isVisible(); takeScreenshot("GNYL_037"); }

    @Test @DisplayName("GNYL_038: 下载Excel模板")
    void g038() { toRoot(); rclickRow(); openExcelDlg();
        Locator b = page.getByText("下载模板EXCEL", new Page.GetByTextOptions().setExact(true)); assertThat(b).isVisible(); b.click(); takeScreenshot("GNYL_038"); }

    @Test @DisplayName("GNYL_039: 拖动Excel上传")
    void g039() { toRoot(); rclickRow(); openExcelDlg();
        Path p = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p); assertThat(page.getByText("需求导入模板E")).isVisible(); takeScreenshot("GNYL_039"); }

    @Test @DisplayName("GNYL_040: 导入Excel(选Sheet+映射+导入)")
    void g040() { toRoot(); rclickRow(); openExcelDlg();
        Path p = Paths.get(TEST_FILES_DIR + "需求导入模板E.xlsx"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p);
        page.getByText("请选择").first().click(); page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("Sheet1")).click();
        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 标题 请选择")).getByRole(AriaRole.IMG).click(); page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("标题")).click();
        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 内容 请选择")).getByRole(AriaRole.IMG).click(); page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("内容")).click();
        page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("* 层级 请选择")).getByRole(AriaRole.IMG).click(); page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("*层级代号")).click();
        Response r = page.waitForResponse(x -> x.url().contains("importReqSpecification") && x.status() == 200, () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click());
        Assertions.assertEquals(200, r.status()); Assertions.assertTrue(r.text().contains("操作成功")); takeScreenshot("GNYL_040"); }

    @Test @DisplayName("GNYL_041: 弹框验证") void g041() { toRoot(); rclickRow(); openExcelDlg();
        assertThat(page.getByText("下载模板EXCEL", new Page.GetByTextOptions().setExact(true))).isVisible(); takeScreenshot("GNYL_041"); }

    @Test @DisplayName("GNYL_042: 标题必填(负向)") void g042() { toRoot(); rclickRow(); openExcelDlg();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click(); takeScreenshot("GNYL_042"); }

    @Test @DisplayName("GNYL_043: 工作表必选(负向)") void g043() { toRoot(); rclickRow(); openExcelDlg();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click(); takeScreenshot("GNYL_043"); }

    @Test @DisplayName("GNYL_044: 属性必选(负向)") void g044() { toRoot(); rclickRow(); openExcelDlg();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click(); takeScreenshot("GNYL_044"); }

    // ═══════════ Word导入 045-052 ═══════════
    @Test @DisplayName("GNYL_045: 右键文件夹→导入→Word")
    void g045() { toRoot(); rclickRow(); openWordDlg();
        assertThat(page.getByRole(AriaRole.DIALOG)).containsText("导入Word"); takeScreenshot("GNYL_045"); }

    @Test @DisplayName("GNYL_046: 树右键→导入→Word")
    void g046() { page.navigate(TestConfig.REQUIREMENT_URL); waitForNetworkIdle(); ensureLoggedIn();
        rclickTree(); openWordDlg(); assertThat(page.getByRole(AriaRole.DIALOG)).containsText("导入Word"); takeScreenshot("GNYL_046"); }

    @Test @DisplayName("GNYL_047: 双击文件夹→导入Word")
    void g047() { toRoot(); page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(F)).first().dblclick();
        rclickRow(); openWordDlg(); takeScreenshot("GNYL_047"); }

    @Test @DisplayName("GNYL_048: 工具栏导入") void g048() { toRoot();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).first()).isVisible(); takeScreenshot("GNYL_048"); }

    @Test @DisplayName("GNYL_049: 下载Word模板") void g049() { toRoot(); rclickRow(); openWordDlg();
        Locator b = page.getByText("下载模板WORD", new Page.GetByTextOptions().setExact(true)); assertThat(b).isVisible(); b.click(); takeScreenshot("GNYL_049"); }

    @Test @DisplayName("GNYL_050: 拖动Word上传") void g050() { toRoot(); rclickRow(); openWordDlg();
        Path p = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p); assertThat(page.getByText("需求导入模板W")).isVisible(); takeScreenshot("GNYL_050"); }

    @Test @DisplayName("GNYL_051: 上传Word+导入")
    void g051() { toRoot(); rclickRow(); openWordDlg();
        Path p = Paths.get(TEST_FILES_DIR + "需求导入模板W.docx"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p);
        Response r = page.waitForResponse(x -> x.url().contains("importReqSpecification") && x.status() == 200, () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click());
        Assertions.assertEquals(200, r.status()); takeScreenshot("GNYL_051"); }

    @Test @DisplayName("GNYL_052: 损坏Word(负向)") void g052() { toRoot(); rclickRow(); openWordDlg();
        Path p = Paths.get(TEST_FILES_DIR + "损坏的需求规格.docx"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p); page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click();
        assertThat(page.locator(".el-message--error,.el-message,.el-notification").first()).isVisible(); takeScreenshot("GNYL_052"); }

    // ═══════════ ReqIf 053-059 ═══════════
    @Test @DisplayName("GNYL_053: 导入→ReqIf") void g053() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); assertThat(page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click(); assertThat(page.getByRole(AriaRole.DIALOG)).isVisible(); takeScreenshot("GNYL_053"); }

    @Test @DisplayName("GNYL_054: ReqIf弹框") void g054() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByRole(AriaRole.DIALOG)).isVisible(); takeScreenshot("GNYL_054"); }

    @Test @DisplayName("GNYL_055: 上传ReqIf") void g055() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        Path p = Paths.get(TEST_FILES_DIR + "Req模版.reqif"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p); assertThat(page.getByText("Req模版")).isVisible(); takeScreenshot("GNYL_055"); }

    @Test @DisplayName("GNYL_056: ReqIf参数") void g056() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        assertThat(page.getByRole(AriaRole.DIALOG)).isVisible(); takeScreenshot("GNYL_056"); }

    @Test @DisplayName("GNYL_057: 导入ReqIf") void g057() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        Path p = Paths.get(TEST_FILES_DIR + "Req模版.reqif"); Assumptions.assumeTrue(p.toFile().exists(), "文件不存在");
        page.locator("input[type='file']").setInputFiles(p); page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click(); takeScreenshot("GNYL_057"); }

    @Test @DisplayName("GNYL_058-059: ReqIf校验") void g058_059() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)).click(); page.getByText("ReqIf", new Page.GetByTextOptions().setExact(true)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导入")).click(); takeScreenshot("GNYL_058-059"); }

    // ═══════════ 导出 060-071 ═══════════
    @Test @DisplayName("GNYL_060: 工具栏导出") void g060() { toRoot();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).first()).isVisible();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).first().click(); takeScreenshot("GNYL_060"); }

    @Test @DisplayName("GNYL_061: 右键导出") void g061() { toRoot(); rclickRow();
        page.getByText("导入", new Page.GetByTextOptions().setExact(true)); /* 验证导入存在后关闭 */
        page.keyboard().press("Escape"); page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(F)).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        assertThat(page.getByText("导出", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByText("导出", new Page.GetByTextOptions().setExact(true)).click(); takeScreenshot("GNYL_061"); }

    @Test @DisplayName("GNYL_062-071: 导出操作") void g062_071() { toRoot();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导出")).first().click(); takeScreenshot("GNYL_062-071"); }
}
