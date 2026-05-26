package cases;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonUITest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(CommonUITest.class);
    private RequirementPage rPage;

    @BeforeAll
    public void init() {
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 需求顶部操作栏通用UI用例 ==========
    // ============================================================
    // TYYL_001: 隐藏文件夹
    // ============================================================
    @Test
    @Order(5001)
    @DisplayName("TYYL_001: 隐藏文件夹")
    public void test_TYYL_001() {
        rPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        // 先确保文件夹可见，再隐藏
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示文件夹")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏文件夹")).click();
        page.waitForTimeout(500);
        log.info("TYYL_001 隐藏文件夹成功");
    }

    // TYYL_002: 显示文件夹
    @Test
    @Order(5002)
    @DisplayName("TYYL_002: 显示文件夹")
    public void test_TYYL_002() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示文件夹")).click();
        page.waitForTimeout(500);
        log.info("TYYL_002 显示文件夹成功");
    }

    // TYYL_003: 隐藏规格
    @Test
    @Order(5003)
    @DisplayName("TYYL_003: 隐藏规格")
    public void test_TYYL_003() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏规格")).click();
        page.waitForTimeout(500);
        log.info("TYYL_003 隐藏规格成功");
    }

    // TYYL_004: 显示规格
    @Test
    @Order(5004)
    @DisplayName("TYYL_004: 显示规格")
    public void test_TYYL_004() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示规格")).click();
        page.waitForTimeout(500);
        log.info("TYYL_004 显示规格成功");
    }

    // TYYL_005: 删除项隐藏
    @Test
    @Order(5005)
    @DisplayName("TYYL_005: 隐藏删除项")
    public void test_TYYL_005() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏删除项")).click();
        page.waitForTimeout(500);
        log.info("TYYL_005 隐藏删除项成功");
    }

    // TYYL_006: 删除项显示
    @Test
    @Order(5006)
    @DisplayName("TYYL_006: 显示删除项")
    public void test_TYYL_006() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示删除项")).click();
        page.waitForTimeout(500);
        log.info("TYYL_006 显示删除项成功");
    }

    // TYYL_007: 定位到指定位置
    @Test
    @Order(5007)
    @DisplayName("TYYL_007: 定位到指定位置")
    public void test_TYYL_007() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("定位")).click();
        page.waitForTimeout(500);
        log.info("TYYL_007 定位成功");
    }

    // TYYL_008: 刷新
    @Test
    @Order(5008)
    @DisplayName("TYYL_008: 刷新")
    public void test_TYYL_008() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷新")).click();
        page.waitForTimeout(1000);
        log.info("TYYL_008 刷新成功");
    }

    // ========== 审签单通用UI用例 ==========
    // 需要: 审签单管理页面的 URL 或导航路径、审签单弹框各字段的选择器
    // ============================================================
    // TYYL_009: 编号必填校验
    // ============================================================
    @Test
    @Order(5009)
    @DisplayName("TYYL_009: 编号必填校验")
    public void test_TYYL_009() {
        // TODO: 填写审签单时，编号字段为空，提交按钮应禁用
        // 需要: 审签单管理页URL、新建审签单弹框、编号输入框、提交按钮的选择器
        log.info("TYYL_009: 编号必填校验 - 待实现，需要审签单页面URL及弹框字段选择器");
    }

    // TYYL_010: 标题必填校验
    @Test
    @Order(5010)
    @DisplayName("TYYL_010: 标题必填校验")
    public void test_TYYL_010() {
        log.info("TYYL_010: 标题必填校验 - 待实现");
    }

    // TYYL_011: 文件上传
    @Test
    @Order(5011)
    @DisplayName("TYYL_011: 文件上传")
    public void test_TYYL_011() {
        log.info("TYYL_011: 文件上传 - 待实现");
    }

    // TYYL_012: 文件大小限制
    @Test
    @Order(5012)
    @DisplayName("TYYL_012: 文件大小限制")
    public void test_TYYL_012() {
        log.info("TYYL_012: 文件大小限制 - 待实现");
    }

    // TYYL_013: 模板选择
    @Test
    @Order(5013)
    @DisplayName("TYYL_013: 模板选择")
    public void test_TYYL_013() {
        log.info("TYYL_013: 模板选择 - 待实现");
    }

    // TYYL_014: 审批人选择
    @Test
    @Order(5014)
    @DisplayName("TYYL_014: 审批人选择")
    public void test_TYYL_014() {
        log.info("TYYL_014: 审批人选择 - 待实现");
    }

    // TYYL_015: 审签单搜索
    @Test
    @Order(5015)
    @DisplayName("TYYL_015: 审签单搜索")
    public void test_TYYL_015() {
        log.info("TYYL_015: 审签单搜索 - 待实现");
    }

    // TYYL_016: 审签单列表展示
    @Test
    @Order(5016)
    @DisplayName("TYYL_016: 审签单列表展示")
    public void test_TYYL_016() {
        log.info("TYYL_016: 审签单列表展示 - 待实现");
    }

    // TYYL_017: 审签单详情查看
    @Test
    @Order(5017)
    @DisplayName("TYYL_017: 审签单详情查看")
    public void test_TYYL_017() {
        log.info("TYYL_017: 审签单详情查看 - 待实现");
    }

    // TYYL_018: 审签单状态筛选
    @Test
    @Order(5018)
    @DisplayName("TYYL_018: 审签单状态筛选")
    public void test_TYYL_018() {
        log.info("TYYL_018: 审签单状态筛选 - 待实现");
    }

    // TYYL_019: 审签单导出
    @Test
    @Order(5019)
    @DisplayName("TYYL_019: 审签单导出")
    public void test_TYYL_019() {
        log.info("TYYL_019: 审签单导出 - 待实现");
    }

    // TYYL_020: 审签单批量操作
    @Test
    @Order(5020)
    @DisplayName("TYYL_020: 审签单批量操作")
    public void test_TYYL_020() {
        log.info("TYYL_020: 审签单批量操作 - 待实现");
    }

    // TYYL_021: 审签单排序
    @Test
    @Order(5021)
    @DisplayName("TYYL_021: 审签单排序")
    public void test_TYYL_021() {
        log.info("TYYL_021: 审签单排序 - 待实现");
    }

    // TYYL_022: 审签单分页
    @Test
    @Order(5022)
    @DisplayName("TYYL_022: 审签单分页")
    public void test_TYYL_022() {
        log.info("TYYL_022: 审签单分页 - 待实现");
    }

    // TYYL_023: 审签单弹窗关闭
    @Test
    @Order(5023)
    @DisplayName("TYYL_023: 审签单弹窗关闭")
    public void test_TYYL_023() {
        log.info("TYYL_023: 审签单弹窗关闭 - 待实现");
    }

    // TYYL_024: 审签单新建
    @Test
    @Order(5024)
    @DisplayName("TYYL_024: 审签单新建")
    public void test_TYYL_024() {
        log.info("TYYL_024: 审签单新建 - 待实现");
    }

    // TYYL_025: 审签单编辑
    @Test
    @Order(5025)
    @DisplayName("TYYL_025: 审签单编辑")
    public void test_TYYL_025() {
        log.info("TYYL_025: 审签单编辑 - 待实现");
    }

    // TYYL_026: 审签单删除
    @Test
    @Order(5026)
    @DisplayName("TYYL_026: 审签单删除")
    public void test_TYYL_026() {
        log.info("TYYL_026: 审签单删除 - 待实现");
    }

    // TYYL_027: 审签单复制
    @Test
    @Order(5027)
    @DisplayName("TYYL_027: 审签单复制")
    public void test_TYYL_027() {
        log.info("TYYL_027: 审签单复制 - 待实现");
    }

    // TYYL_028: 审签单提交
    @Test
    @Order(5028)
    @DisplayName("TYYL_028: 审签单提交")
    public void test_TYYL_028() {
        log.info("TYYL_028: 审签单提交 - 待实现");
    }

    // TYYL_029: 审签单撤回
    @Test
    @Order(5029)
    @DisplayName("TYYL_029: 审签单撤回")
    public void test_TYYL_029() {
        log.info("TYYL_029: 审签单撤回 - 待实现");
    }

    // TYYL_030: 审签单审批
    @Test
    @Order(5030)
    @DisplayName("TYYL_030: 审签单审批")
    public void test_TYYL_030() {
        log.info("TYYL_030: 审签单审批 - 待实现");
    }

    // TYYL_031: 审签单拒绝
    @Test
    @Order(5031)
    @DisplayName("TYYL_031: 审签单拒绝")
    public void test_TYYL_031() {
        log.info("TYYL_031: 审签单拒绝 - 待实现");
    }

    // TYYL_032: 审签单完成
    @Test
    @Order(5032)
    @DisplayName("TYYL_032: 审签单完成")
    public void test_TYYL_032() {
        log.info("TYYL_032: 审签单完成 - 待实现");
    }

    // TYYL_033: 审签单归档
    @Test
    @Order(5033)
    @DisplayName("TYYL_033: 审签单归档")
    public void test_TYYL_033() {
        log.info("TYYL_033: 审签单归档 - 待实现");
    }

    // TYYL_034: 审签单取消归档
    @Test
    @Order(5034)
    @DisplayName("TYYL_034: 审签单取消归档")
    public void test_TYYL_034() {
        log.info("TYYL_034: 审签单取消归档 - 待实现");
    }

    // TYYL_035: 审签单查询历史
    @Test
    @Order(5035)
    @DisplayName("TYYL_035: 审签单查询历史")
    public void test_TYYL_035() {
        log.info("TYYL_035: 审签单查询历史 - 待实现");
    }

    // TYYL_036: 审签单批量审批
    @Test
    @Order(5036)
    @DisplayName("TYYL_036: 审签单批量审批")
    public void test_TYYL_036() {
        log.info("TYYL_036: 审签单批量审批 - 待实现");
    }

    // TYYL_037: 审签单批量拒绝
    @Test
    @Order(5037)
    @DisplayName("TYYL_037: 审签单批量拒绝")
    public void test_TYYL_037() {
        log.info("TYYL_037: 审签单批量拒绝 - 待实现");
    }

    // TYYL_038: 审签单批量完成
    @Test
    @Order(5038)
    @DisplayName("TYYL_038: 审签单批量完成")
    public void test_TYYL_038() {
        log.info("TYYL_038: 审签单批量完成 - 待实现");
    }

    // TYYL_039: 审签单批量归档
    @Test
    @Order(5039)
    @DisplayName("TYYL_039: 审签单批量归档")
    public void test_TYYL_039() {
        log.info("TYYL_039: 审签单批量归档 - 待实现");
    }

    // TYYL_040: 审签单批量取消归档
    @Test
    @Order(5040)
    @DisplayName("TYYL_040: 审签单批量取消归档")
    public void test_TYYL_040() {
        log.info("TYYL_040: 审签单批量取消归档 - 待实现");
    }

    // ========== 导入弹框通用UI用例 ==========
    // ============================================================
    // TYYL_041: 上传文件删除
    // ============================================================
    @Test
    @Order(5041)
    @DisplayName("TYYL_041: 上传文件删除")
    public void test_TYYL_041() {
        // 打开文件夹，在导入弹框中删除已上传的文件
        rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
        page.waitForTimeout(1000);

        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);

            // 查找文件上传输入框并上传一个文件
            Locator fileInput = page.locator("input[type='file']").first();
            if (fileInput.isVisible()) {
                fileInput.setInputFiles(Paths.get("src/main/resources/application.properties"));
                page.waitForTimeout(1000);
            }

            // 删除已上传的文件
            Locator deleteBtn = page.locator(".el-upload-list__item .el-icon-close, [class*='file-delete'], .el-icon-delete").first();
            if (deleteBtn.isVisible()) {
                deleteBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_041 上传文件删除成功");
            } else {
                log.info("TYYL_041 未找到已上传文件的删除按钮");
            }

            // 关闭弹框
            Locator cancelBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消"));
            if (!cancelBtn.isVisible()) {
                cancelBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("取消")).first();
            }
            if (cancelBtn.isVisible()) {
                cancelBtn.click();
                page.waitForTimeout(500);
            }
        } else {
            log.info("TYYL_041 未找到导入按钮");
        }
    }

    // ========== 追溯视图通用UI用例 ==========
    // 需要: 追溯视图页面的导航路径、全屏/缩放/下载/打印/分享等操作按钮的选择器
    // ============================================================
    // TYYL_042: 全屏展示
    // ============================================================
    @Test
    @Order(5042)
    @DisplayName("TYYL_042: 全屏展示")
    public void test_TYYL_042() {
        log.info("TYYL_042: 全屏展示 - 待实现，需要追溯视图的全屏按钮选择器");
    }

    // TYYL_043: 放大缩小
    @Test
    @Order(5043)
    @DisplayName("TYYL_043: 放大缩小")
    public void test_TYYL_043() {
        log.info("TYYL_043: 放大缩小 - 待实现");
    }

    // TYYL_044: 自适应
    @Test
    @Order(5044)
    @DisplayName("TYYL_044: 自适应")
    public void test_TYYL_044() {
        log.info("TYYL_044: 自适应 - 待实现");
    }

    // TYYL_045: 下载
    @Test
    @Order(5045)
    @DisplayName("TYYL_045: 下载")
    public void test_TYYL_045() {
        log.info("TYYL_045: 下载 - 待实现");
    }

    // TYYL_046: 打印
    @Test
    @Order(5046)
    @DisplayName("TYYL_046: 打印")
    public void test_TYYL_046() {
        log.info("TYYL_046: 打印 - 待实现");
    }

    // TYYL_047: 分享
    @Test
    @Order(5047)
    @DisplayName("TYYL_047: 分享")
    public void test_TYYL_047() {
        log.info("TYYL_047: 分享 - 待实现");
    }

    // TYYL_048: 刷新
    @Test
    @Order(5048)
    @DisplayName("TYYL_048: 刷新")
    public void test_TYYL_048() {
        Locator refreshBtn = page.locator("[title*='刷新'], [class*='refresh'], .el-icon-refresh").first();
        if (refreshBtn.isVisible()) {
            refreshBtn.click();
            page.waitForTimeout(1000);
            log.info("TYYL_048 追溯视图刷新成功");
        } else {
            log.info("TYYL_048: 刷新 - 待实现，需要追溯视图的刷新按钮选择器");
        }
    }

    // ========== 分页及每页条数通用UI用例 ==========
    // ============================================================
    // TYYL_049: 分页导航
    // ============================================================
    @Test
    @Order(5049)
    @DisplayName("TYYL_049: 分页导航")
    public void test_TYYL_049() {
        Locator pagination = page.locator(".el-pagination").first();
        if (!pagination.isVisible()) {
            rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
            page.waitForTimeout(1000);
            pagination = page.locator(".el-pagination").first();
        }

        if (pagination.isVisible()) {
            Locator nextBtn = pagination.locator(".btn-next").first();
            if (nextBtn.isVisible() && !nextBtn.getAttribute("class").contains("disabled")) {
                nextBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_049 下一页点击成功");
            } else {
                log.info("TYYL_049 下一页按钮不可用（已禁用或不存在）");
            }

            Locator prevBtn = pagination.locator(".btn-prev").first();
            if (prevBtn.isVisible() && !prevBtn.getAttribute("class").contains("disabled")) {
                prevBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_049 上一页点击成功");
            } else {
                log.info("TYYL_049 上一页按钮不可用");
            }

            log.info("TYYL_049 分页导航测试完成");
        } else {
            log.info("TYYL_049 未找到分页组件");
        }
    }

    // TYYL_050: 页码跳转
    @Test
    @Order(5050)
    @DisplayName("TYYL_050: 页码跳转")
    public void test_TYYL_050() {
        Locator pagination = page.locator(".el-pagination").first();
        if (!pagination.isVisible()) {
            rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
            page.waitForTimeout(1000);
            pagination = page.locator(".el-pagination").first();
        }

        if (pagination.isVisible()) {
            // Element UI 分页跳转输入框
            Locator jumpInput = pagination.locator("input[type='text']").first();
            if (jumpInput.isVisible()) {
                jumpInput.click();
                jumpInput.fill("1");
                jumpInput.press("Enter");
                page.waitForTimeout(500);
                log.info("TYYL_050 页码跳转成功");
            } else {
                log.info("TYYL_050 未找到跳转输入框");
            }
        } else {
            log.info("TYYL_050 未找到分页组件");
        }
    }

    // TYYL_051: 每页条数切换
    @Test
    @Order(5051)
    @DisplayName("TYYL_051: 每页条数切换")
    public void test_TYYL_051() {
        Locator pagination = page.locator(".el-pagination").first();
        if (!pagination.isVisible()) {
            rPage.doubleClickTreeNode(TestConstants.PARENT_FOLDER);
            page.waitForTimeout(1000);
            pagination = page.locator(".el-pagination").first();
        }

        if (pagination.isVisible()) {
            Locator pageSizeSelect = pagination.locator(".el-select").first();
            if (pageSizeSelect.isVisible()) {
                pageSizeSelect.click();
                page.waitForTimeout(300);

                Locator option = page.locator(".el-select-dropdown__item").first();
                if (option.isVisible()) {
                    option.click();
                    page.waitForTimeout(500);
                    log.info("TYYL_051 每页条数切换成功");
                } else {
                    log.info("TYYL_051 未找到下拉选项");
                }
            } else {
                log.info("TYYL_051 未找到每页条数下拉框");
            }
        } else {
            log.info("TYYL_051 未找到分页组件");
        }
    }

    // ========== 弹框通用UI用例 ==========
    // 需要打开一个对话框来测试，这里使用右键菜单 → 属性的方式打开对话框
    // ============================================================
    // TYYL_052: 弹窗关闭
    // ============================================================
    private void openSampleDialog() {
        rPage.rightClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);
    }

    @Test
    @Order(5052)
    @DisplayName("TYYL_052: 弹窗关闭")
    public void test_TYYL_052() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator closeBtn = dialog.locator(".el-dialog__close, .el-dialog__headerbtn, [class*='close']").first();
            if (closeBtn.isVisible()) {
                closeBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_052 弹窗关闭成功");
            } else {
                log.info("TYYL_052 未找到关闭按钮");
            }
        } else {
            log.info("TYYL_052 弹窗未打开");
        }
    }

    // TYYL_053: 弹窗确认
    @Test
    @Order(5053)
    @DisplayName("TYYL_053: 弹窗确认")
    public void test_TYYL_053() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator confirmBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定"));
            if (!confirmBtn.isVisible()) {
                confirmBtn = dialog.locator("button").filter(new Locator.FilterOptions().setHasText("确定")).first();
            }
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_053 弹窗确认成功");
            } else {
                log.info("TYYL_053 未找到确认按钮");
            }
        } else {
            log.info("TYYL_053 弹窗未打开");
        }
    }

    // TYYL_054: 弹窗取消
    @Test
    @Order(5054)
    @DisplayName("TYYL_054: 弹窗取消")
    public void test_TYYL_054() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator cancelBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消"));
            if (!cancelBtn.isVisible()) {
                cancelBtn = dialog.locator("button").filter(new Locator.FilterOptions().setHasText("取消")).first();
            }
            if (cancelBtn.isVisible()) {
                cancelBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_054 弹窗取消成功");
            } else {
                log.info("TYYL_054 未找到取消按钮");
            }
        } else {
            log.info("TYYL_054 弹窗未打开");
        }
    }

    // TYYL_055: 弹窗最大最小化
    @Test
    @Order(5055)
    @DisplayName("TYYL_055: 弹窗最大最小化")
    public void test_TYYL_055() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator maxBtn = page.locator("[class*='maximize'], [class*='max'], .el-icon-full-screen").first();
            if (maxBtn.isVisible()) {
                maxBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_055 弹窗最大化成功");
            } else {
                log.info("TYYL_055 未找到最大化按钮");
            }

            Locator minBtn = page.locator("[class*='minimize'], [class*='min'], .el-icon-minus").first();
            if (minBtn.isVisible()) {
                minBtn.click();
                page.waitForTimeout(500);
                log.info("TYYL_055 弹窗最小化成功");
            } else {
                log.info("TYYL_055 未找到最小化按钮");
            }

            Locator closeBtn = dialog.locator(".el-dialog__close, .el-dialog__headerbtn").first();
            if (closeBtn.isVisible()) closeBtn.click();
            page.waitForTimeout(300);
        } else {
            log.info("TYYL_055 弹窗未打开");
        }
    }

    // TYYL_056: 弹窗拖拽
    @Test
    @Order(5056)
    @DisplayName("TYYL_056: 弹窗拖拽")
    public void test_TYYL_056() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator header = dialog.locator(".el-dialog__header, .el-dialog__title").first();
            if (header.isVisible()) {
                // 获取当前位置，然后拖拽一段距离
                header.hover();
                page.mouse().down();
                page.mouse().move(100, 50);
                page.mouse().up();
                page.waitForTimeout(500);
                log.info("TYYL_056 弹窗拖拽成功");
            } else {
                log.info("TYYL_056 未找到弹窗标题栏");
            }

            Locator closeBtn = dialog.locator(".el-dialog__close, .el-dialog__headerbtn").first();
            if (closeBtn.isVisible()) closeBtn.click();
            page.waitForTimeout(300);
        } else {
            log.info("TYYL_056 弹窗未打开");
        }
    }

    // TYYL_057: 弹窗尺寸调整
    @Test
    @Order(5057)
    @DisplayName("TYYL_057: 弹窗尺寸调整")
    public void test_TYYL_057() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            // 尝试在弹窗右下角拖拽调整大小
            Locator resizeHandle = page.locator("[class*='resize'], [class*='handle']").first();
            if (resizeHandle.isVisible()) {
                resizeHandle.hover();
                page.mouse().down();
                page.mouse().move(50, 50);
                page.mouse().up();
                page.waitForTimeout(500);
                log.info("TYYL_057 弹窗尺寸调整成功");
            } else {
                log.info("TYYL_057 未找到尺寸调整手柄");
            }

            Locator closeBtn = dialog.locator(".el-dialog__close, .el-dialog__headerbtn").first();
            if (closeBtn.isVisible()) closeBtn.click();
            page.waitForTimeout(300);
        } else {
            log.info("TYYL_057 弹窗未打开");
        }
    }

    // TYYL_058: 弹窗内容滚动
    @Test
    @Order(5058)
    @DisplayName("TYYL_058: 弹窗内容滚动")
    public void test_TYYL_058() {
        openSampleDialog();

        Locator dialog = page.locator(".el-dialog").first();
        if (dialog.isVisible()) {
            Locator body = dialog.locator(".el-dialog__body").first();
            if (body.isVisible()) {
                // 判断内容是否可滚动
                Object result = body.evaluate("el => el.scrollHeight > el.clientHeight", null);
                boolean scrollable = result instanceof Boolean && (Boolean) result;
                if (scrollable) {
                    body.evaluate("el => el.scrollTop = el.scrollHeight", null);
                    page.waitForTimeout(300);
                    log.info("TYYL_058 弹窗内容滚动成功");
                } else {
                    log.info("TYYL_058 弹窗内容无需滚动（内容未溢出）");
                }
            } else {
                log.info("TYYL_058 未找到弹窗内容区域");
            }

            Locator closeBtn = dialog.locator(".el-dialog__close, .el-dialog__headerbtn").first();
            if (closeBtn.isVisible()) closeBtn.click();
            page.waitForTimeout(300);
        } else {
            log.info("TYYL_058 弹窗未打开");
        }
    }

}
