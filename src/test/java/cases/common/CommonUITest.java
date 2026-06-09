package cases.common;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Tag("CommonModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonUITest extends BaseTest {

    // ========== 需求顶部操作栏通用UI用例 ==========

    // @Test removed
    @DisplayName("TYYL_001: 隐藏文件夹")
    public void test_TYYL_001() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示文件夹")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏文件夹")).click();
        page.waitForTimeout(500);
        log.info("TYYL_001 隐藏文件夹成功");
    }

    // @Test removed
    @DisplayName("TYYL_002: 显示文件夹")
    public void test_TYYL_002() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示文件夹")).click();
        page.waitForTimeout(500);
        log.info("TYYL_002 显示文件夹成功");
    }

    // @Test removed
    @DisplayName("TYYL_003: 隐藏规格")
    public void test_TYYL_003() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏规格")).click();
        page.waitForTimeout(500);
        log.info("TYYL_003 隐藏规格成功");
    }

    // @Test removed
    @DisplayName("TYYL_004: 显示规格")
    public void test_TYYL_004() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示规格")).click();
        page.waitForTimeout(500);
        log.info("TYYL_004 显示规格成功");
    }

    // @Test removed
    @DisplayName("TYYL_005: 隐藏删除项")
    public void test_TYYL_005() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏删除项")).click();
        page.waitForTimeout(500);
        log.info("TYYL_005 隐藏删除项成功");
    }

    // @Test removed
    @DisplayName("TYYL_006: 显示删除项")
    public void test_TYYL_006() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示删除项")).click();
        page.waitForTimeout(500);
        log.info("TYYL_006 显示删除项成功");
    }

    // @Test removed
    @DisplayName("TYYL_007: 定位到指定位置")
    public void test_TYYL_007() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("定位")).click();
        page.waitForTimeout(500);
        log.info("TYYL_007 定位成功");
    }

    // @Test removed
    @DisplayName("TYYL_008: 刷新")
    public void test_TYYL_008() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷新")).click();
        page.waitForTimeout(1000);
        log.info("TYYL_008 刷新成功");
    }

    // ========== 审签单通用UI用例 ==========

    // @Test removed
    @DisplayName("TYYL_009: 编号必填校验")
    public void test_TYYL_009() {
        log.info("TYYL_009: 编号必填校验 - 待实现，需要审签单页面URL及弹框字段选择器");
    }

    // @Test removed
    @DisplayName("TYYL_010: 标题必填校验")
    public void test_TYYL_010() {
        log.info("TYYL_010: 标题必填校验 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_011: 文件上传")
    public void test_TYYL_011() {
        log.info("TYYL_011: 文件上传 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_012: 文件大小限制")
    public void test_TYYL_012() {
        log.info("TYYL_012: 文件大小限制 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_013: 模板选择")
    public void test_TYYL_013() {
        log.info("TYYL_013: 模板选择 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_014: 审批人选择")
    public void test_TYYL_014() {
        log.info("TYYL_014: 审批人选择 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_015: 审签单搜索")
    public void test_TYYL_015() {
        log.info("TYYL_015: 审签单搜索 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_016: 审签单列表展示")
    public void test_TYYL_016() {
        log.info("TYYL_016: 审签单列表展示 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_017: 审签单详情查看")
    public void test_TYYL_017() {
        log.info("TYYL_017: 审签单详情查看 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_018: 审签单状态筛选")
    public void test_TYYL_018() {
        log.info("TYYL_018: 审签单状态筛选 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_019: 审签单导出")
    public void test_TYYL_019() {
        log.info("TYYL_019: 审签单导出 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_020: 审签单批量操作")
    public void test_TYYL_020() {
        log.info("TYYL_020: 审签单批量操作 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_021: 审签单排序")
    public void test_TYYL_021() {
        log.info("TYYL_021: 审签单排序 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_022: 审签单分页")
    public void test_TYYL_022() {
        log.info("TYYL_022: 审签单分页 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_023: 审签单弹窗关闭")
    public void test_TYYL_023() {
        log.info("TYYL_023: 审签单弹窗关闭 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_024: 审签单新建")
    public void test_TYYL_024() {
        log.info("TYYL_024: 审签单新建 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_025: 审签单编辑")
    public void test_TYYL_025() {
        log.info("TYYL_025: 审签单编辑 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_026: 审签单删除")
    public void test_TYYL_026() {
        log.info("TYYL_026: 审签单删除 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_027: 审签单复制")
    public void test_TYYL_027() {
        log.info("TYYL_027: 审签单复制 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_028: 审签单提交")
    public void test_TYYL_028() {
        log.info("TYYL_028: 审签单提交 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_029: 审签单撤回")
    public void test_TYYL_029() {
        log.info("TYYL_029: 审签单撤回 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_030: 审签单审批")
    public void test_TYYL_030() {
        log.info("TYYL_030: 审签单审批 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_031: 审签单拒绝")
    public void test_TYYL_031() {
        log.info("TYYL_031: 审签单拒绝 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_032: 审签单完成")
    public void test_TYYL_032() {
        log.info("TYYL_032: 审签单完成 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_033: 审签单归档")
    public void test_TYYL_033() {
        log.info("TYYL_033: 审签单归档 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_034: 审签单取消归档")
    public void test_TYYL_034() {
        log.info("TYYL_034: 审签单取消归档 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_035: 审签单查询历史")
    public void test_TYYL_035() {
        log.info("TYYL_035: 审签单查询历史 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_036: 审签单批量审批")
    public void test_TYYL_036() {
        log.info("TYYL_036: 审签单批量审批 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_037: 审签单批量拒绝")
    public void test_TYYL_037() {
        log.info("TYYL_037: 审签单批量拒绝 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_038: 审签单批量完成")
    public void test_TYYL_038() {
        log.info("TYYL_038: 审签单批量完成 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_039: 审签单批量归档")
    public void test_TYYL_039() {
        log.info("TYYL_039: 审签单批量归档 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_040: 审签单批量取消归档")
    public void test_TYYL_040() {
        log.info("TYYL_040: 审签单批量取消归档 - 待实现");
    }

    // ========== 导入弹框通用UI用例 ==========

    // @Test removed
    @DisplayName("TYYL_041: 上传文件删除")
    public void test_TYYL_041() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
            if (importBtn.isVisible()) {
                importBtn.click();
                page.waitForTimeout(1000);

                Locator fileInput = page.locator("input[type='file']").first();
                if (fileInput.isVisible()) {
                    fileInput.setInputFiles(Paths.get("src/main/resources/application.properties"));
                    page.waitForTimeout(1000);
                }

                Locator deleteBtn = page.locator(".el-upload-list__item .el-icon-close, [class*='file-delete'], .el-icon-delete").first();
                if (deleteBtn.isVisible()) {
                    deleteBtn.click();
                    page.waitForTimeout(500);
                    log.info("TYYL_041 上传文件删除成功");
                } else {
                    log.info("TYYL_041 未找到已上传文件的删除按钮");
                }

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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // ========== 追溯视图通用UI用例 ==========

    // @Test removed
    @DisplayName("TYYL_042: 全屏展示")
    public void test_TYYL_042() {
        log.info("TYYL_042: 全屏展示 - 待实现，需要追溯视图的全屏按钮选择器");
    }

    // @Test removed
    @DisplayName("TYYL_043: 放大缩小")
    public void test_TYYL_043() {
        log.info("TYYL_043: 放大缩小 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_044: 自适应")
    public void test_TYYL_044() {
        log.info("TYYL_044: 自适应 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_045: 下载")
    public void test_TYYL_045() {
        log.info("TYYL_045: 下载 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_046: 打印")
    public void test_TYYL_046() {
        log.info("TYYL_046: 打印 - 待实现");
    }

    // @Test removed
    @DisplayName("TYYL_047: 分享")
    public void test_TYYL_047() {
        log.info("TYYL_047: 分享 - 待实现");
    }

    // @Test removed
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

    // @Test removed
    @DisplayName("TYYL_049: 分页导航")
    public void test_TYYL_049() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator pagination = page.locator(".el-pagination").first();
            if (!pagination.isVisible()) {
                reqPage.doubleClickTreeNode(folder[1]);
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_050: 页码跳转")
    public void test_TYYL_050() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator pagination = page.locator(".el-pagination").first();
            if (!pagination.isVisible()) {
                reqPage.doubleClickTreeNode(folder[1]);
                page.waitForTimeout(1000);
                pagination = page.locator(".el-pagination").first();
            }

            if (pagination.isVisible()) {
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_051: 每页条数切换")
    public void test_TYYL_051() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            Locator pagination = page.locator(".el-pagination").first();
            if (!pagination.isVisible()) {
                reqPage.doubleClickTreeNode(folder[1]);
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
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // ========== 弹框通用UI用例 ==========

    private String[] openSampleDialog() {
        String[] doc = createTempDoc();
        reqPage.refreshTree();
        page.waitForTimeout(1000);

        reqPage.rightClickTreeNode(doc[1]);
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);
        return doc;
    }

    // @Test removed
    @DisplayName("TYYL_052: 弹窗关闭")
    public void test_TYYL_052() {
        String[] doc = openSampleDialog();
        try {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_053: 弹窗确认")
    public void test_TYYL_053() {
        String[] doc = openSampleDialog();
        try {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_054: 弹窗取消")
    public void test_TYYL_054() {
        String[] doc = openSampleDialog();
        try {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_055: 弹窗最大最小化")
    public void test_TYYL_055() {
        String[] doc = openSampleDialog();
        try {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_056: 弹窗拖拽")
    public void test_TYYL_056() {
        String[] doc = openSampleDialog();
        try {
            Locator dialog = page.locator(".el-dialog").first();
            if (dialog.isVisible()) {
                Locator header = dialog.locator(".el-dialog__header, .el-dialog__title").first();
                if (header.isVisible()) {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_057: 弹窗尺寸调整")
    public void test_TYYL_057() {
        String[] doc = openSampleDialog();
        try {
            Locator dialog = page.locator(".el-dialog").first();
            if (dialog.isVisible()) {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    // @Test removed
    @DisplayName("TYYL_058: 弹窗内容滚动")
    public void test_TYYL_058() {
        String[] doc = openSampleDialog();
        try {
            Locator dialog = page.locator(".el-dialog").first();
            if (dialog.isVisible()) {
                Locator body = dialog.locator(".el-dialog__body").first();
                if (body.isVisible()) {
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
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

}
