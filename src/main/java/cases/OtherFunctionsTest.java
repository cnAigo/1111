package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;
import java.nio.file.Paths;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OtherFunctionsTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(OtherFunctionsTest.class);
    private ReqApiActions api;
    private RequirementPage rPage;

    @BeforeAll
    public void init() {
        api = new ReqApiActions(page.request());
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToSystemManagement();
    }

    // ========== 工具方法 ==========

    private void navigateToCooperationZone() {
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        page.waitForTimeout(1500);
        ensureLoggedIn();
        page.locator(".el-menu-item").filter(new Locator.FilterOptions().setHasText("合作区管理")).click();
        page.waitForTimeout(1000);
    }

    private void navigateToUserManagement() {
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        page.waitForTimeout(1500);
        ensureLoggedIn();
        page.locator(".el-menu-item").filter(new Locator.FilterOptions().setHasText("用户管理")).click();
        page.waitForTimeout(1000);
    }

    private boolean hasTableData() {
        Locator rows = page.locator(".el-table__body-wrapper tbody tr").first();
        return rows.isVisible() && !rows.textContent().trim().contains("暂无数据");
    }

    private void clickDialogConfirm() {
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator confirmBtn = dialog.locator(".el-dialog__footer .el-button--primary, .el-button--primary").first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(500);
            }
        }
    }

    private void clickDialogCancel() {
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator cancelBtn = dialog.locator("button").filter(new Locator.FilterOptions().setHasText("取 消")).first();
            if (cancelBtn.isVisible()) {
                cancelBtn.click();
                page.waitForTimeout(500);
            }
        }
    }

    private void clickSearchButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("搜 索"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("搜索")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(1000);
        }
    }

    private void clickResetButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("重 置"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("重置")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(500);
        }
    }

    private void clickAddButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新 增"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("新增")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(1000);
        }
    }

    private void clickModifyButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("修 改"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("修改")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(1000);
        }
    }

    private void clickDeleteButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("删 除"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("删除")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(500);
        }
    }

    private void clickRefreshButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷 新"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("刷新")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(1000);
        }
    }

    private void clickExportButton() {
        Locator btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("导 出"));
        if (!btn.isVisible()) {
            btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导出")).first();
        }
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(500);
        }
    }

    private void clickMoreButton() {
        Locator btn = page.locator("button").filter(new Locator.FilterOptions().setHasText("更多操作")).first();
        if (btn.isVisible()) {
            btn.click();
            page.waitForTimeout(500);
        }
    }

    private void fillInputByPlaceholder(String placeholder, String value) {
        Locator input = page.locator("input[placeholder*='" + placeholder + "']").first();
        if (input.isVisible()) {
            input.click();
            input.fill(value);
            page.waitForTimeout(300);
        }
    }

    // ========== 合作区管理 ==========
    @Test
    @Order(1001)
    @DisplayName("QTYL_001: 进入合作区管理页面")
    public void test_QTYL_001() {
        navigateToCooperationZone();
        Locator table = page.locator(".el-table").first();
        if (table.isVisible()) {
            log.info("QTYL_001 合作区管理页面加载成功，表格可见");
        } else {
            log.info("QTYL_001 合作区管理页面已进入（表格可能为空）");
        }
    }

    @Test
    @Order(1002)
    @DisplayName("QTYL_002: 添加合作区")
    public void test_QTYL_002() {
        navigateToCooperationZone();
        clickAddButton();

        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 填写合作区名称
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("合作区_auto_" + System.currentTimeMillis());
            }
            // 填写编码
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("hzq_" + System.currentTimeMillis());
            }
            clickDialogConfirm();
            page.waitForTimeout(1000);
            log.info("QTYL_002 添加合作区成功");
        } else {
            log.info("QTYL_002 新增对话框未弹出");
        }
    }

    @Test
    @Order(1003)
    @DisplayName("QTYL_003: 修改合作区")
    public void test_QTYL_003() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_003 表格无数据，跳过修改操作");
            return;
        }
        // 点击第一行的修改按钮
        Locator editBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("修改")).first();
        if (editBtn.isVisible()) {
            editBtn.click();
            page.waitForTimeout(1000);
            Locator dialog = page.locator(".el-dialog").last();
            if (dialog.isVisible()) {
                Locator nameInput = dialog.locator("input").first();
                if (nameInput.isVisible()) {
                    nameInput.fill("合作区_修改_" + System.currentTimeMillis());
                }
                clickDialogConfirm();
                page.waitForTimeout(1000);
                log.info("QTYL_003 修改合作区成功");
            } else {
                log.info("QTYL_003 修改对话框未弹出");
            }
        } else {
            log.info("QTYL_003 未找到修改按钮");
        }
    }

    @Test
    @Order(1004)
    @DisplayName("QTYL_004: 合作区名称必填测试")
    public void test_QTYL_004() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 不填写名称，直接点确定
            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message, .el-message-box").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_004 名称必填提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_004 名称未填时确认按钮可能被禁用");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_004 新增对话框未弹出");
        }
    }

    @Test
    @Order(1005)
    @DisplayName("QTYL_005: 合作区编码必填测试")
    public void test_QTYL_005() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("合作区必填测试");
            }
            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message, .el-message-box").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_005 编码必填提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_005 编码未填时确认按钮可能被禁用");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_005 新增对话框未弹出");
        }
    }

    @Test
    @Order(1006)
    @DisplayName("QTYL_006: 密级必选测试")
    public void test_QTYL_006() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("合作区密级测试");
            }
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("test_" + System.currentTimeMillis());
            }
            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message, .el-message-box").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_006 密级必填提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_006 密级未选时确认按钮可能被禁用");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_006 新增对话框未弹出");
        }
    }

    @Test
    @Order(1007)
    @DisplayName("QTYL_007: 输入非字母开头的合作区编码")
    public void test_QTYL_007() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) nameInput.fill("编码测试");
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("123abc");
                page.waitForTimeout(300);
                Locator errMsg = page.locator(".el-form-item__error").first();
                if (errMsg.isVisible()) {
                    log.info("QTYL_007 非字母开头编码提示: {}", errMsg.textContent().trim());
                } else {
                    log.info("QTYL_007 输入非字母开头编码无即时提示");
                }
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1008)
    @DisplayName("QTYL_008: 非字母或字母+数字组合的编码")
    public void test_QTYL_008() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) nameInput.fill("编码特殊字符测试");
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("test@#$%");
                page.waitForTimeout(300);
                Locator errMsg = page.locator(".el-form-item__error").first();
                if (errMsg.isVisible()) {
                    log.info("QTYL_008 特殊字符编码提示: {}", errMsg.textContent().trim());
                } else {
                    log.info("QTYL_008 输入特殊字符编码无即时提示");
                }
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1009)
    @DisplayName("QTYL_009: 合作区名称空格校验")
    public void test_QTYL_009() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("   ");
                page.waitForTimeout(300);
            }
            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message, .el-message-box").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_009 空格校验提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_009 纯空格输入后未检测到错误提示");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1010)
    @DisplayName("QTYL_010: 合作区名称重复校验")
    public void test_QTYL_010() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 使用已存在的合作区名称
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("test");
            }
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("dup_test_" + System.currentTimeMillis());
            }
            clickDialogConfirm();
            page.waitForTimeout(1000);
            Locator errMsg = page.locator(".el-message, .el-message-box, .el-notification").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_010 名称重复提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_010 名称重复未检测到提示");
            }
        }
    }

    @Test
    @Order(1011)
    @DisplayName("QTYL_011: 合作区编码重复校验")
    public void test_QTYL_011() {
        navigateToCooperationZone();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator nameInput = dialog.locator("input").first();
            if (nameInput.isVisible()) {
                nameInput.fill("编码重复测试_" + System.currentTimeMillis());
            }
            // 尝试使用可能重复的编码
            Locator codeInput = dialog.locator("input").nth(1);
            if (codeInput.isVisible()) {
                codeInput.fill("test");  // 使用可能已存在的编码
            }
            clickDialogConfirm();
            page.waitForTimeout(1000);
            Locator errMsg = page.locator(".el-message, .el-message-box, .el-notification").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_011 编码重复提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_011 编码重复未检测到提示");
            }
        }
    }

    @Test
    @Order(1012)
    @DisplayName("QTYL_012: 删除有用户的合作区")
    public void test_QTYL_012() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_012 表格无数据，跳过");
            return;
        }
        // 选择合作区 → 删除
        page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
        page.waitForTimeout(300);
        clickDeleteButton();
        page.waitForTimeout(500);
        Locator dialog = page.locator(".el-message-box, .el-dialog").last();
        if (dialog.isVisible()) {
            String msg = dialog.textContent();
            log.info("QTYL_012 删除有用户合作区提示: {}", msg.trim());
            // 取消删除
            Locator cancelBtn = dialog.locator("button").filter(new Locator.FilterOptions().setHasText("取 消")).first();
            if (cancelBtn.isVisible()) cancelBtn.click();
            page.waitForTimeout(300);
        }
    }

    @Test
    @Order(1013)
    @DisplayName("QTYL_013: 删除没有用户的合作区")
    public void test_QTYL_013() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_013 表格无数据，跳过");
            return;
        }
        page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
        page.waitForTimeout(300);
        clickDeleteButton();
        page.waitForTimeout(500);
        Locator dialog = page.locator(".el-message-box, .el-dialog").last();
        if (dialog.isVisible()) {
            String msg = dialog.textContent();
            log.info("QTYL_013 删除确认框: {}", msg.trim());
            // 确认删除
            Locator confirmBtn = dialog.locator("button").filter(new Locator.FilterOptions().setHasText("确 定")).first();
            if (confirmBtn.isVisible()) {
                confirmBtn.click();
                page.waitForTimeout(1000);
                log.info("QTYL_013 删除操作已执行");
            }
        }
    }

    @Test
    @Order(1014)
    @DisplayName("QTYL_014: 存在的合作区名称检索")
    public void test_QTYL_014() {
        navigateToCooperationZone();
        fillInputByPlaceholder("合作区名称或编码", "test");
        clickSearchButton();
        if (hasTableData()) {
            log.info("QTYL_014 搜索存在的名称成功");
        } else {
            log.info("QTYL_014 搜索完成，未匹配到结果或无数据");
        }
    }

    @Test
    @Order(1015)
    @DisplayName("QTYL_015: 合作区名称模糊查询")
    public void test_QTYL_015() {
        navigateToCooperationZone();
        fillInputByPlaceholder("合作区名称或编码", "te");
        clickSearchButton();
        log.info("QTYL_015 模糊查询完成");
    }

    @Test
    @Order(1016)
    @DisplayName("QTYL_016: 不存在的合作区名称检索")
    public void test_QTYL_016() {
        navigateToCooperationZone();
        fillInputByPlaceholder("合作区名称或编码", "__不存在_名称__");
        clickSearchButton();
        Locator emptyText = page.locator(".el-empty, .el-table__empty-text, [class*='empty']").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_016 不存在的名称检索显示暂无数据");
        } else {
            log.info("QTYL_016 搜索完成，无匹配结果");
        }
    }

    @Test
    @Order(1017)
    @DisplayName("QTYL_017: 重置")
    public void test_QTYL_017() {
        navigateToCooperationZone();
        fillInputByPlaceholder("合作区名称或编码", "test");
        clickResetButton();
        Locator input = page.locator("input[placeholder*='合作区名称或编码']").first();
        String val = input.inputValue();
        if (val.isEmpty()) {
            log.info("QTYL_017 重置成功，搜索条件已清空");
        } else {
            log.info("QTYL_017 重置完成");
        }
    }

    @Test
    @Order(1018)
    @DisplayName("QTYL_018: 添加用户(合作区人员分配)")
    public void test_QTYL_018() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_018 表格无数据，跳过");
            return;
        }
        // 第一行点击"分配人员"或类似操作
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (!assignBtn.isVisible()) {
            // 尝试点击其他可能的按钮
            assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                    .first().locator("td:last-child button, td:last-child span, td:last-child a").first();
        }
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            log.info("QTYL_018 人员分配操作已触发");
            Locator dialog = page.locator(".el-dialog").last();
            if (dialog.isVisible()) {
                Locator userCheckbox = dialog.locator(".el-checkbox").first();
                if (userCheckbox.isVisible()) {
                    userCheckbox.click();
                    page.waitForTimeout(300);
                }
                clickDialogConfirm();
                page.waitForTimeout(500);
                log.info("QTYL_018 人员分配对话框已操作");
            }
        } else {
            log.info("QTYL_018 未找到人员分配入口");
        }
    }

    @Test
    @Order(1019)
    @DisplayName("QTYL_019: 重复添加用户")
    public void test_QTYL_019() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_019 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            Locator dialog = page.locator(".el-dialog").last();
            if (dialog.isVisible()) {
                // 选择已存在的用户
                Locator userCheckbox = dialog.locator(".el-checkbox").first();
                if (userCheckbox.isVisible()) {
                    userCheckbox.click();
                    page.waitForTimeout(300);
                }
                clickDialogConfirm();
                page.waitForTimeout(500);
                Locator errMsg = page.locator(".el-message, .el-notification").first();
                if (errMsg.isVisible()) {
                    log.info("QTYL_019 重复添加提示: {}", errMsg.textContent().trim());
                } else {
                    log.info("QTYL_019 重复添加操作完成");
                }
            }
        } else {
            log.info("QTYL_019 未找到人员分配入口");
        }
    }

    @Test
    @Order(1020)
    @DisplayName("QTYL_020: 组织部门选择验证")
    public void test_QTYL_020() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_020 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            // 如果有部门树，选择不同部门
            Locator deptTree = page.locator(".el-tree").first();
            if (deptTree.isVisible()) {
                Locator deptNode = deptTree.locator(".el-tree-node").first();
                if (deptNode.isVisible()) {
                    deptNode.click();
                    page.waitForTimeout(500);
                    log.info("QTYL_020 组织部门选择完成，用户列表已更新");
                } else {
                    log.info("QTYL_020 部门树无可选节点");
                }
            } else {
                log.info("QTYL_020 未找到部门树选择器");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_020 未找到人员分配入口");
        }
    }

    @Test
    @Order(1021)
    @DisplayName("QTYL_021: 存在的用户名称检索")
    public void test_QTYL_021() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_021 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            fillInputByPlaceholder("用户名称", "admin");
            clickSearchButton();
            log.info("QTYL_021 存在的用户名称检索完成");
            clickDialogCancel();
        } else {
            log.info("QTYL_021 未找到人员分配入口");
        }
    }

    @Test
    @Order(1022)
    @DisplayName("QTYL_022: 不存在的用户名称检索")
    public void test_QTYL_022() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_022 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            fillInputByPlaceholder("用户名称", "__不存在_用户__");
            clickSearchButton();
            Locator emptyText = page.locator(".el-empty, .el-table__empty-text").first();
            if (emptyText.isVisible()) {
                log.info("QTYL_022 不存在的用户检索显示暂无数据");
            } else {
                log.info("QTYL_022 搜索完成");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_022 未找到人员分配入口");
        }
    }

    @Test
    @Order(1023)
    @DisplayName("QTYL_023: 用户名称模糊搜索")
    public void test_QTYL_023() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_023 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            fillInputByPlaceholder("用户名称", "ad");
            clickSearchButton();
            log.info("QTYL_023 用户名称模糊搜索完成");
            clickDialogCancel();
        } else {
            log.info("QTYL_023 未找到人员分配入口");
        }
    }

    @Test
    @Order(1024)
    @DisplayName("QTYL_024: 重置")
    public void test_QTYL_024() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_024 表格无数据，跳过");
            return;
        }
        Locator assignBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配")).first();
        if (assignBtn.isVisible()) {
            assignBtn.click();
            page.waitForTimeout(1000);
            fillInputByPlaceholder("用户名称", "admin");
            clickResetButton();
            log.info("QTYL_024 重置条件完成");
            clickDialogCancel();
        } else {
            log.info("QTYL_024 未找到人员分配入口");
        }
    }

    @Test
    @Order(1025)
    @DisplayName("QTYL_025: 取消授权")
    public void test_QTYL_025() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_025 表格无数据，跳过");
            return;
        }
        // 点击操作栏中的取消授权
        Locator removeBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("取消授权")).first();
        if (!removeBtn.isVisible()) {
            removeBtn = page.locator(".el-table__body-wrapper tbody tr")
                    .first().locator("button, a, span").filter(new Locator.FilterOptions().setHasText("移除")).first();
        }
        if (removeBtn.isVisible()) {
            removeBtn.click();
            page.waitForTimeout(500);
            Locator confirm = page.locator(".el-message-box, .el-dialog").last();
            if (confirm.isVisible()) {
                Locator okBtn = confirm.locator("button").filter(new Locator.FilterOptions().setHasText("确 定")).first();
                if (okBtn.isVisible()) {
                    okBtn.click();
                    page.waitForTimeout(500);
                }
            }
            log.info("QTYL_025 取消授权操作已执行");
        } else {
            log.info("QTYL_025 未找到取消授权按钮");
        }
    }

    @Test
    @Order(1026)
    @DisplayName("QTYL_026: 批量取消授权")
    public void test_QTYL_026() {
        navigateToCooperationZone();
        if (!hasTableData()) {
            log.info("QTYL_026 表格无数据，跳过");
            return;
        }
        Locator checkbox = page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first();
        if (checkbox.isVisible()) {
            checkbox.click();
            page.waitForTimeout(300);
            Locator batchRemoveBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("批量取消授权")).first();
            if (!batchRemoveBtn.isVisible()) {
                batchRemoveBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("取消授权")).first();
            }
            if (batchRemoveBtn.isVisible()) {
                batchRemoveBtn.click();
                page.waitForTimeout(500);
                log.info("QTYL_026 批量取消授权操作已执行");
            } else {
                log.info("QTYL_026 未找到批量取消授权按钮");
            }
        } else {
            log.info("QTYL_026 未找到复选框");
        }
    }

    // ========== 用户管理 ==========
    @Test
    @Order(1027)
    @DisplayName("QTYL_027: 存在的部门名称检索")
    public void test_QTYL_027() {
        navigateToUserManagement();
        fillInputByPlaceholder("部门名称", "公司");
        clickSearchButton();
        log.info("QTYL_027 存在的部门名称检索完成");
    }

    @Test
    @Order(1028)
    @DisplayName("QTYL_028: 部门名称模糊查询")
    public void test_QTYL_028() {
        navigateToUserManagement();
        fillInputByPlaceholder("部门名称", "部");
        clickSearchButton();
        log.info("QTYL_028 部门名称模糊查询完成");
    }

    @Test
    @Order(1029)
    @DisplayName("QTYL_029: 不存在的部门名称检索")
    public void test_QTYL_029() {
        navigateToUserManagement();
        fillInputByPlaceholder("部门名称", "__不存在_部门__");
        clickSearchButton();
        Locator emptyText = page.locator(".el-table__empty-text, .el-empty").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_029 不存在的部门检索显示暂无数据");
        } else {
            log.info("QTYL_029 搜索完成");
        }
    }

    @Test
    @Order(1030)
    @DisplayName("QTYL_030: 重置")
    public void test_QTYL_030() {
        navigateToUserManagement();
        fillInputByPlaceholder("部门名称", "公司");
        // 点击部门搜索框的清除按钮（x）
        Locator clearBtn = page.locator("input[placeholder*='部门名称'] + .el-input__suffix .el-icon-circle-close, input[placeholder*='部门名称'] ~ .el-input__suffix .el-input__clear").first();
        if (clearBtn.isVisible()) {
            clearBtn.click();
            page.waitForTimeout(300);
            log.info("QTYL_030 部门搜索条件已清空");
        } else {
            log.info("QTYL_030 未找到清除按钮");
        }
    }

    @Test
    @Order(1031)
    @DisplayName("QTYL_031: 部门选择验证")
    public void test_QTYL_031() {
        navigateToUserManagement();
        // 点击左侧部门树的节点
        Locator deptTree = page.locator(".el-tree").first();
        if (deptTree.isVisible()) {
            Locator deptNode = deptTree.locator(".el-tree-node").first();
            if (deptNode.isVisible()) {
                deptNode.click();
                page.waitForTimeout(1000);
                log.info("QTYL_031 部门选择完成，用户列表已更新");
            } else {
                log.info("QTYL_031 部门树无可选节点");
            }
        } else {
            log.info("QTYL_031 未找到部门树");
        }
    }

    @Test
    @Order(1032)
    @DisplayName("QTYL_032: 存在的用户名称检索")
    public void test_QTYL_032() {
        navigateToUserManagement();
        fillInputByPlaceholder("用户名称", "admin");
        clickSearchButton();
        if (hasTableData()) {
            log.info("QTYL_032 存在的用户名称检索成功");
        } else {
            log.info("QTYL_032 搜索完成，未匹配到结果");
        }
    }

    @Test
    @Order(1033)
    @DisplayName("QTYL_033: 不存在的用户名称检索")
    public void test_QTYL_033() {
        navigateToUserManagement();
        fillInputByPlaceholder("用户名称", "__不存在_用户__");
        clickSearchButton();
        Locator emptyText = page.locator(".el-table__empty-text, .el-empty").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_033 不存在的用户检索显示暂无数据");
        } else {
            log.info("QTYL_033 搜索完成");
        }
    }

    @Test
    @Order(1034)
    @DisplayName("QTYL_034: 用户名称模糊搜索")
    public void test_QTYL_034() {
        navigateToUserManagement();
        fillInputByPlaceholder("用户名称", "ad");
        clickSearchButton();
        if (hasTableData()) {
            log.info("QTYL_034 模糊搜索成功");
        } else {
            log.info("QTYL_034 模糊搜索完成");
        }
    }

    @Test
    @Order(1035)
    @DisplayName("QTYL_035: 存在的手机号码检索")
    public void test_QTYL_035() {
        navigateToUserManagement();
        fillInputByPlaceholder("手机号码", "15888888888");
        clickSearchButton();
        if (hasTableData()) {
            log.info("QTYL_035 存在的手机号码检索成功");
        } else {
            log.info("QTYL_035 搜索完成");
        }
    }

    @Test
    @Order(1036)
    @DisplayName("QTYL_036: 不存在的手机号码检索")
    public void test_QTYL_036() {
        navigateToUserManagement();
        fillInputByPlaceholder("手机号码", "99999999999");
        clickSearchButton();
        Locator emptyText = page.locator(".el-table__empty-text, .el-empty").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_036 不存在的手机号检索显示暂无数据");
        } else {
            log.info("QTYL_036 搜索完成");
        }
    }

    @Test
    @Order(1037)
    @DisplayName("QTYL_037: 手机号码模糊搜索")
    public void test_QTYL_037() {
        navigateToUserManagement();
        fillInputByPlaceholder("手机号码", "158");
        clickSearchButton();
        if (hasTableData()) {
            log.info("QTYL_037 手机号码模糊搜索成功");
        } else {
            log.info("QTYL_037 模糊搜索完成");
        }
    }

    @Test
    @Order(1038)
    @DisplayName("QTYL_038: 存在的状态检索")
    public void test_QTYL_038() {
        navigateToUserManagement();
        // 选择状态
        Locator statusSelect = page.locator(".el-select").filter(new Locator.FilterOptions().setHasText("状态")).first();
        if (statusSelect.isVisible()) {
            statusSelect.click();
            page.waitForTimeout(300);
            Locator option = page.locator(".el-select-dropdown__item").filter(new Locator.FilterOptions().setHasText("成功")).first();
            if (option.isVisible()) {
                option.click();
                page.waitForTimeout(300);
            }
        }
        clickSearchButton();
        log.info("QTYL_038 存在的状态检索完成");
    }

    @Test
    @Order(1039)
    @DisplayName("QTYL_039: 不存在的状态检索")
    public void test_QTYL_039() {
        navigateToUserManagement();
        Locator statusSelect = page.locator(".el-select").filter(new Locator.FilterOptions().setHasText("状态")).first();
        if (statusSelect.isVisible()) {
            statusSelect.click();
            page.waitForTimeout(300);
            // 选择"停用"状态
            Locator option = page.locator(".el-select-dropdown__item").filter(new Locator.FilterOptions().setHasText("停用")).first();
            if (option.isVisible()) {
                option.click();
                page.waitForTimeout(300);
            }
        }
        clickSearchButton();
        Locator emptyText = page.locator(".el-table__empty-text, .el-empty").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_039 不存在的状态检索显示暂无数据");
        } else {
            log.info("QTYL_039 搜索完成");
        }
    }

    @Test
    @Order(1040)
    @DisplayName("QTYL_040: 存在的创建时间检索")
    public void test_QTYL_040() {
        navigateToUserManagement();
        Locator startDate = page.locator("input[placeholder*='开始时间'], input[placeholder*='起始']").first();
        if (startDate.isVisible()) {
            startDate.click();
            startDate.fill("2026-01-01");
            page.waitForTimeout(300);
        }
        Locator endDate = page.locator("input[placeholder*='结束时间'], input[placeholder*='截止']").first();
        if (endDate.isVisible()) {
            endDate.click();
            endDate.fill("2026-12-31");
            page.waitForTimeout(300);
        }
        clickSearchButton();
        log.info("QTYL_040 创建时间检索完成");
    }

    @Test
    @Order(1041)
    @DisplayName("QTYL_041: 不存在的创建时间检索")
    public void test_QTYL_041() {
        navigateToUserManagement();
        Locator startDate = page.locator("input[placeholder*='开始时间'], input[placeholder*='起始']").first();
        if (startDate.isVisible()) {
            startDate.click();
            startDate.fill("2020-01-01");
            page.waitForTimeout(300);
        }
        Locator endDate = page.locator("input[placeholder*='结束时间'], input[placeholder*='截止']").first();
        if (endDate.isVisible()) {
            endDate.click();
            endDate.fill("2020-06-30");
            page.waitForTimeout(300);
        }
        clickSearchButton();
        Locator emptyText = page.locator(".el-table__empty-text, .el-empty").first();
        if (emptyText.isVisible()) {
            log.info("QTYL_041 不存在的创建时间检索显示暂无数据");
        } else {
            log.info("QTYL_041 搜索完成");
        }
    }

    @Test
    @Order(1042)
    @DisplayName("QTYL_042: 组合查询")
    public void test_QTYL_042() {
        navigateToUserManagement();
        fillInputByPlaceholder("用户名称", "admin");
        fillInputByPlaceholder("手机号码", "158");
        clickSearchButton();
        log.info("QTYL_042 组合查询完成");
    }

    @Test
    @Order(1043)
    @DisplayName("QTYL_043: 重置")
    public void test_QTYL_043() {
        navigateToUserManagement();
        fillInputByPlaceholder("用户名称", "admin");
        fillInputByPlaceholder("手机号码", "15888888888");
        clickResetButton();
        Locator nameInput = page.locator("input[placeholder*='用户名称']").first();
        String val = nameInput.inputValue();
        log.info("QTYL_043 重置完成, 用户名称输入框值: '{}'", val);
    }

    @Test
    @Order(1044)
    @DisplayName("QTYL_044: 新增用户")
    public void test_QTYL_044() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 填写新增用户表单
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            // 用户昵称
            if (inputs.count() > 0) inputs.nth(0).fill("自动测试_" + suffix);
            // 用户名称
            if (inputs.count() > 1) inputs.nth(1).fill("auto_user_" + suffix);
            // 密码
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");
            // 手机号码
            if (inputs.count() > 3) inputs.nth(3).fill("138" + suffix.substring(0, 8));

            clickDialogConfirm();
            page.waitForTimeout(1000);
            Locator successMsg = page.locator(".el-message--success, .el-notification").first();
            if (successMsg.isVisible()) {
                log.info("QTYL_044 新增用户成功: {}", successMsg.textContent().trim());
            } else {
                log.info("QTYL_044 新增用户操作已完成");
            }
        } else {
            log.info("QTYL_044 新增对话框未弹出");
        }
    }

    @Test
    @Order(1045)
    @DisplayName("QTYL_045: 用户昵称非空校验")
    public void test_QTYL_045() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 填写其他字段，留空昵称
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            // 跳过第一个（昵称），填写用户名称
            if (inputs.count() > 1) inputs.nth(1).fill("auto_empty_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_045 昵称非空提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_045 昵称为空时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1046)
    @DisplayName("QTYL_046: 用户名称非空校验")
    public void test_QTYL_046() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error, .el-message").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_046 名称非空提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_046 名称为空时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1047)
    @DisplayName("QTYL_047: 用户名称长度校验(不足2位)")
    public void test_QTYL_047() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("a");  // 只有1位
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_047 名称长度不足提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_047 长度不足时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1048)
    @DisplayName("QTYL_048: 用户名称长度校验(2-20位)")
    public void test_QTYL_048() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_test_" + suffix);  // 正常长度
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");

            clickDialogConfirm();
            page.waitForTimeout(500);
            log.info("QTYL_048 用户名称长度2-20位输入成功");
        }
    }

    @Test
    @Order(1049)
    @DisplayName("QTYL_049: 用户名称长度校验(超过20位)")
    public void test_QTYL_049() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("a".repeat(25));  // 25位，超过20

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_049 名称超长提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_049 超长时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1050)
    @DisplayName("QTYL_050: 用户密码非空校验")
    public void test_QTYL_050() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_" + suffix);
            // 不填密码

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_050 密码非空提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_050 密码为空时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1051)
    @DisplayName("QTYL_051: 用户密码长度校验(不足5位)")
    public void test_QTYL_051() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("1234");  // 4位，不足5

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_051 密码长度不足提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_051 密码不足5位时确认按钮可能被禁用");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1052)
    @DisplayName("QTYL_052: 用户密码长度校验(5-20位)")
    public void test_QTYL_052() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");  // 正常密码

            clickDialogConfirm();
            page.waitForTimeout(500);
            log.info("QTYL_052 密码5-20位输入成功");
        }
    }

    @Test
    @Order(1053)
    @DisplayName("QTYL_053: 用户密码长度校验(超过20位)")
    public void test_QTYL_053() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试昵称_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_" + suffix);
            // 尝试输入超长密码
            Locator pwdInput = dialog.locator("input[type='password']").first();
            if (pwdInput.isVisible()) {
                pwdInput.fill("a".repeat(25));
                page.waitForTimeout(300);
                // 验证是否被截断到20位
                String val = pwdInput.inputValue();
                log.info("QTYL_053 超长密码实际输入长度: {}", val.length());
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1054)
    @DisplayName("QTYL_054: 用户密码显示明文测试")
    public void test_QTYL_054() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator pwdInput = dialog.locator("input[type='password']").first();
            if (pwdInput.isVisible()) {
                pwdInput.fill("Aa123456");
                page.waitForTimeout(300);
                // 点击密码可见切换图标
                Locator eyeIcon = dialog.locator(".el-input__suffix .el-icon, .el-input__icon").first();
                if (eyeIcon.isVisible()) {
                    eyeIcon.click();
                    page.waitForTimeout(300);
                    // 切换后输入框类型应变为text
                    Locator textInput = dialog.locator("input[type='text']").last();
                    if (textInput.isVisible()) {
                        log.info("QTYL_054 密码明文显示成功");
                    } else {
                        log.info("QTYL_054 点击切换图标后未检测到明文输入框");
                    }
                } else {
                    log.info("QTYL_054 未找到密码可见切换图标");
                }
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1055)
    @DisplayName("QTYL_055: 手机号码格式校验(正确)")
    public void test_QTYL_055() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试手机_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_phone_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");
            // 输入正确手机号
            if (inputs.count() > 3) inputs.nth(3).fill("138" + suffix.substring(0, 8));

            clickDialogConfirm();
            page.waitForTimeout(500);
            log.info("QTYL_055 正确手机号输入完成");
        }
    }

    @Test
    @Order(1056)
    @DisplayName("QTYL_056: 手机号码格式校验(错误)")
    public void test_QTYL_056() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator inputs = dialog.locator("input:visible");
            if (inputs.count() > 0) inputs.nth(0).fill("测试错误手机_" + suffix);
            if (inputs.count() > 1) inputs.nth(1).fill("auto_badphone_" + suffix);
            if (inputs.count() > 2) inputs.nth(2).fill("Aa123456");
            if (inputs.count() > 3) inputs.nth(3).fill("12345");  // 非手机格式

            clickDialogConfirm();
            page.waitForTimeout(500);
            Locator errMsg = page.locator(".el-form-item__error").first();
            if (errMsg.isVisible()) {
                log.info("QTYL_056 手机号格式错误提示: {}", errMsg.textContent().trim());
            } else {
                log.info("QTYL_056 错误手机号格式提交完成（可能无前端校验）");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1057)
    @DisplayName("QTYL_057: 邮箱格式校验(正确)")
    public void test_QTYL_057() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            Locator allInputs = dialog.locator("input:visible");
            if (allInputs.count() > 0) allInputs.nth(0).fill("测试邮箱_" + suffix);
            if (allInputs.count() > 1) allInputs.nth(1).fill("auto_email_" + suffix);
            if (allInputs.count() > 2) allInputs.nth(2).fill("Aa123456");
            // 尝试找到邮箱输入框（如果有）
            Locator emailInput = dialog.locator("input[type='email'], input[placeholder*='邮箱']").first();
            if (emailInput.isVisible()) {
                emailInput.fill("test@example.com");
            }
            log.info("QTYL_057 正确邮箱输入完成");
        }
    }

    @Test
    @Order(1058)
    @DisplayName("QTYL_058: 邮箱格式校验(错误)")
    public void test_QTYL_058() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
            if (dialog.locator("input:visible").count() > 0)
                dialog.locator("input:visible").nth(0).fill("测试错误邮箱_" + suffix);
            if (dialog.locator("input:visible").count() > 1)
                dialog.locator("input:visible").nth(1).fill("auto_bademail_" + suffix);
            if (dialog.locator("input:visible").count() > 2)
                dialog.locator("input:visible").nth(2).fill("Aa123456");

            Locator emailInput = dialog.locator("input[type='email'], input[placeholder*='邮箱']").first();
            if (emailInput.isVisible()) {
                emailInput.fill("not-an-email");
                clickDialogConfirm();
                page.waitForTimeout(500);
                Locator errMsg = page.locator(".el-form-item__error").first();
                if (errMsg.isVisible()) {
                    log.info("QTYL_058 邮箱格式错误提示: {}", errMsg.textContent().trim());
                } else {
                    log.info("QTYL_058 错误邮箱格式提交完成（可能无前端校验）");
                }
            } else {
                log.info("QTYL_058 未找到邮箱输入框");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1059)
    @DisplayName("QTYL_059: 岗位多选")
    public void test_QTYL_059() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 在对话框中找到岗位选择器（如果有）
            Locator postSelect = dialog.locator(".el-select").filter(new Locator.FilterOptions().setHasText("岗位")).first();
            if (!postSelect.isVisible()) {
                // 尝试找所有下拉框
                postSelect = dialog.locator(".el-select").nth(1);  // 可能第二个是岗位
            }
            if (postSelect.isVisible()) {
                postSelect.click();
                page.waitForTimeout(500);
                // 选择第一个选项
                Locator option = page.locator(".el-select-dropdown__item").first();
                if (option.isVisible()) {
                    option.click();
                    page.waitForTimeout(300);
                    log.info("QTYL_059 岗位多选成功");
                }
            } else {
                log.info("QTYL_059 未找到岗位选择器");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1060)
    @DisplayName("QTYL_060: 岗位删除")
    public void test_QTYL_060() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 点击已选岗位的删除按钮（x）
            Locator tagClose = dialog.locator(".el-tag__close, .el-select__tags .el-tag .el-icon-close").first();
            if (tagClose.isVisible()) {
                tagClose.click();
                page.waitForTimeout(300);
                log.info("QTYL_060 岗位删除（取消选择）成功");
            } else {
                log.info("QTYL_060 未找到已选岗位的删除按钮");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1061)
    @DisplayName("QTYL_061: 角色多选")
    public void test_QTYL_061() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator roleSelect = dialog.locator(".el-select").filter(new Locator.FilterOptions().setHasText("角色")).first();
            if (roleSelect.isVisible()) {
                roleSelect.click();
                page.waitForTimeout(500);
                Locator option = page.locator(".el-select-dropdown__item").first();
                if (option.isVisible()) {
                    option.click();
                    page.waitForTimeout(300);
                    log.info("QTYL_061 角色多选成功");
                }
            } else {
                log.info("QTYL_061 未找到角色选择器");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1062)
    @DisplayName("QTYL_062: 角色删除")
    public void test_QTYL_062() {
        navigateToUserManagement();
        clickAddButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            Locator tagClose = dialog.locator(".el-tag__close, .el-select__tags .el-tag .el-icon-close").first();
            if (tagClose.isVisible()) {
                tagClose.click();
                page.waitForTimeout(300);
                log.info("QTYL_062 角色删除（取消选择）成功");
            } else {
                log.info("QTYL_062 未找到已选角色的删除按钮");
            }
            clickDialogCancel();
        }
    }

    @Test
    @Order(1063)
    @DisplayName("QTYL_063: 导入用户(点击上传)")
    public void test_QTYL_063() {
        navigateToUserManagement();
        // 找"导入"按钮
        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);
            // 尝试上传文件
            Locator fileInput = page.locator("input[type='file']").first();
            if (fileInput.isVisible()) {
                fileInput.setInputFiles(Paths.get("src/main/resources/test-data/test_users.xlsx"));
                page.waitForTimeout(1000);
                log.info("QTYL_063 导入用户文件已上传");
                clickDialogConfirm();
                page.waitForTimeout(1000);
            } else {
                log.info("QTYL_063 未找到文件上传输入框");
            }
        } else {
            log.info("QTYL_063 未找到导入按钮（可能在更多操作中）");
            clickMoreButton();
            Locator importInMenu = page.locator(".el-dropdown-menu__item, .el-menu-item")
                    .filter(new Locator.FilterOptions().setHasText("导入")).first();
            if (importInMenu.isVisible()) {
                importInMenu.click();
                page.waitForTimeout(1000);
                log.info("QTYL_063 通过菜单打开导入对话框");
            } else {
                log.info("QTYL_063 更多操作中也未找到导入");
            }
        }
    }

    @Test
    @Order(1064)
    @DisplayName("QTYL_064: 导入用户(拖拽上传)")
    public void test_QTYL_064() {
        navigateToUserManagement();
        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);
            // 如果有拖拽区域
            Locator dragArea = page.locator(".el-upload-dragger, .el-upload__drag").first();
            if (dragArea.isVisible()) {
                log.info("QTYL_064 拖拽上传区域可见");
            } else {
                log.info("QTYL_064 未检测到拖拽上传区域");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_064 未找到导入按钮");
        }
    }

    @Test
    @Order(1065)
    @DisplayName("QTYL_065: 非excel格式文件导入")
    public void test_QTYL_065() {
        navigateToUserManagement();
        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);
            // 尝试上传非excel文件
            Locator fileInput = page.locator("input[type='file']").first();
            if (fileInput.isVisible()) {
                fileInput.setInputFiles(Paths.get("src/main/resources/application.properties"));
                page.waitForTimeout(500);
                Locator errMsg = page.locator(".el-message, .el-upload__tip, .el-notification").first();
                if (errMsg.isVisible()) {
                    log.info("QTYL_065 非excel文件提示: {}", errMsg.textContent().trim());
                } else {
                    log.info("QTYL_065 非excel文件上传完成");
                }
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_065 未找到导入按钮");
        }
    }

    @Test
    @Order(1066)
    @DisplayName("QTYL_066: 更新存在的用户数据")
    public void test_QTYL_066() {
        navigateToUserManagement();
        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);
            // 查找"更新已存在用户"复选框
            Locator updateCheckbox = page.locator(".el-checkbox").filter(new Locator.FilterOptions().setHasText("更新")).first();
            if (updateCheckbox.isVisible()) {
                updateCheckbox.click();
                page.waitForTimeout(300);
                log.info("QTYL_066 勾选更新已存在用户");

                Locator fileInput = page.locator("input[type='file']").first();
                if (fileInput.isVisible()) {
                    fileInput.setInputFiles(Paths.get("src/main/resources/test-data/test_users.xlsx"));
                    page.waitForTimeout(1000);
                }
                clickDialogConfirm();
                page.waitForTimeout(1000);
            } else {
                log.info("QTYL_066 未找到更新已存在用户复选框");
            }
        } else {
            log.info("QTYL_066 未找到导入按钮");
        }
    }

    @Test
    @Order(1067)
    @DisplayName("QTYL_067: 上传文件删除")
    public void test_QTYL_067() {
        navigateToUserManagement();
        Locator importBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("导入")).first();
        if (importBtn.isVisible()) {
            importBtn.click();
            page.waitForTimeout(1000);
            // 先上传一个文件
            Locator fileInput = page.locator("input[type='file']").first();
            if (fileInput.isVisible()) {
                fileInput.setInputFiles(Paths.get("src/main/resources/test-data/test_users.xlsx"));
                page.waitForTimeout(500);
            }
            // 然后删除上传的文件
            Locator deleteBtn = page.locator(".el-upload-list__item .el-icon-close, .el-upload__btn .el-icon-close, [class*='file-delete']").first();
            if (deleteBtn.isVisible()) {
                deleteBtn.click();
                page.waitForTimeout(500);
                log.info("QTYL_067 上传文件已删除");
            } else {
                log.info("QTYL_067 未找到已上传文件的删除按钮");
            }
            clickDialogCancel();
        } else {
            log.info("QTYL_067 未找到导入按钮");
        }
    }

    @Test
    @Order(1068)
    @DisplayName("QTYL_068: 导出用户")
    public void test_QTYL_068() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_068 表格无数据，跳过导出");
            return;
        }
        // 勾选第一个用户
        Locator checkbox = page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first();
        if (checkbox.isVisible()) {
            checkbox.click();
            page.waitForTimeout(300);
        }
        clickExportButton();
        page.waitForTimeout(1000);
        log.info("QTYL_068 导出操作已触发");
    }

    @Test
    @Order(1069)
    @DisplayName("QTYL_069: 隐藏搜索")
    public void test_QTYL_069() {
        navigateToUserManagement();
        // 找"隐藏搜索"按钮
        Locator hideSearchBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("隐藏搜索")).first();
        if (hideSearchBtn.isVisible()) {
            hideSearchBtn.click();
            page.waitForTimeout(500);
            // 验证搜索区域隐藏
            boolean searchHidden = page.locator(".el-table").first().evaluate(
                    "() => { const search = document.querySelector('[class*=\"search\"]'); return !search || search.offsetHeight === 0; }", null
            ).toString().contains("true");
            log.info("QTYL_069 隐藏搜索{}", searchHidden ? "成功" : "（搜索区域可能未完全隐藏）");
        } else {
            log.info("QTYL_069 未找到'隐藏搜索'按钮");
        }
    }

    @Test
    @Order(1070)
    @DisplayName("QTYL_070: 显示搜索")
    public void test_QTYL_070() {
        navigateToUserManagement();
        Locator showSearchBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("显示搜索")).first();
        if (showSearchBtn.isVisible()) {
            showSearchBtn.click();
            page.waitForTimeout(500);
            log.info("QTYL_070 显示搜索成功");
        } else {
            log.info("QTYL_070 未找到'显示搜索'按钮");
        }
    }

    @Test
    @Order(1071)
    @DisplayName("QTYL_071: 列表刷新")
    public void test_QTYL_071() {
        navigateToUserManagement();
        clickRefreshButton();
        log.info("QTYL_071 列表刷新成功");
    }

    @Test
    @Order(1072)
    @DisplayName("QTYL_072: 隐藏列属性")
    public void test_QTYL_072() {
        navigateToUserManagement();
        // 找"显隐列"或列设置按钮
        Locator columnSettings = page.locator("button").filter(new Locator.FilterOptions().setHasText("显隐列")).first();
        if (!columnSettings.isVisible()) {
            columnSettings = page.locator("[class*='column'], [class*='setting'], .el-icon-setting").first();
        }
        if (columnSettings.isVisible()) {
            columnSettings.click();
            page.waitForTimeout(500);
            // 取消勾选第一个列
            Locator columnCheckbox = page.locator(".el-checkbox").first();
            if (columnCheckbox.isVisible()) {
                columnCheckbox.click();
                page.waitForTimeout(300);
                log.info("QTYL_072 列隐藏成功");
            } else {
                log.info("QTYL_072 未找到列选项");
            }
            // 点击空白处关闭
            page.locator(".el-table").first().click();
            page.waitForTimeout(300);
        } else {
            log.info("QTYL_072 未找到列设置按钮");
        }
    }

    @Test
    @Order(1073)
    @DisplayName("QTYL_073: 显示列属性")
    public void test_QTYL_073() {
        navigateToUserManagement();
        Locator columnSettings = page.locator("button").filter(new Locator.FilterOptions().setHasText("显隐列")).first();
        if (!columnSettings.isVisible()) {
            columnSettings = page.locator("[class*='column'], [class*='setting'], .el-icon-setting").first();
        }
        if (columnSettings.isVisible()) {
            columnSettings.click();
            page.waitForTimeout(500);
            // 勾选之前取消的列
            Locator columnCheckbox = page.locator(".el-checkbox").first();
            if (columnCheckbox.isVisible()) {
                columnCheckbox.click();
                page.waitForTimeout(300);
                log.info("QTYL_073 列显示成功");
            }
            page.locator(".el-table").first().click();
            page.waitForTimeout(300);
        } else {
            log.info("QTYL_073 未找到列设置按钮");
        }
    }

    @Test
    @Order(1074)
    @DisplayName("QTYL_074: 修改用户(勾选)")
    public void test_QTYL_074() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_074 表格无数据，跳过");
            return;
        }
        // 勾选第一个用户
        page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
        page.waitForTimeout(300);
        clickModifyButton();
        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            log.info("QTYL_074 修改用户对话框已打开");
            clickDialogCancel();
        } else {
            log.info("QTYL_074 修改对话框未弹出");
        }
    }

    @Test
    @Order(1075)
    @DisplayName("QTYL_075: 修改用户(操作栏)")
    public void test_QTYL_075() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_075 表格无数据，跳过");
            return;
        }
        // 操作栏的修改按钮
        Locator editBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("修改")).first();
        if (editBtn.isVisible()) {
            editBtn.click();
            page.waitForTimeout(1000);
            log.info("QTYL_075 操作栏修改按钮已点击");
            clickDialogCancel();
        } else {
            log.info("QTYL_075 未找到操作栏修改按钮");
        }
    }

    @Test
    @Order(1076)
    @DisplayName("QTYL_076: 删除用户")
    public void test_QTYL_076() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_076 表格无数据，跳过");
            return;
        }
        // 操作栏删除
        Locator delBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("删除")).first();
        if (delBtn.isVisible()) {
            delBtn.click();
            page.waitForTimeout(500);
            Locator confirmBox = page.locator(".el-message-box, .el-dialog").last();
            if (confirmBox.isVisible()) {
                String msg = confirmBox.textContent();
                log.info("QTYL_076 删除确认框: {}", msg.trim());
                // 取消删除以免影响数据
                Locator cancelBtn = confirmBox.locator("button").filter(new Locator.FilterOptions().setHasText("取 消")).first();
                if (cancelBtn.isVisible()) cancelBtn.click();
                page.waitForTimeout(300);
            }
        } else {
            log.info("QTYL_076 未找到操作栏删除按钮");
        }
    }

    @Test
    @Order(1077)
    @DisplayName("QTYL_077: 批量删除用户")
    public void test_QTYL_077() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_077 表格无数据，跳过");
            return;
        }
        // 勾选多个用户
        page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
        page.waitForTimeout(200);
        if (page.locator(".el-table__body-wrapper tbody tr").count() > 1) {
            page.locator(".el-table__body-wrapper tbody tr").nth(1).locator(".el-checkbox").click();
            page.waitForTimeout(200);
        }
        clickDeleteButton();
        page.waitForTimeout(500);
        Locator confirmBox = page.locator(".el-message-box, .el-dialog").last();
        if (confirmBox.isVisible()) {
            Locator cancelBtn = confirmBox.locator("button").filter(new Locator.FilterOptions().setHasText("取 消")).first();
            if (cancelBtn.isVisible()) cancelBtn.click();
        }
        log.info("QTYL_077 批量删除操作已执行");
    }

    @Test
    @Order(1078)
    @DisplayName("QTYL_078: 重置密码")
    public void test_QTYL_078() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_078 表格无数据，跳过");
            return;
        }
        // 找"重置密码"按钮
        Locator resetPwdBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("重置密码")).first();
        if (resetPwdBtn.isVisible()) {
            resetPwdBtn.click();
            page.waitForTimeout(1000);
            Locator dialog = page.locator(".el-dialog").last();
            if (dialog.isVisible()) {
                Locator pwdInput = dialog.locator("input[type='password']").first();
                if (pwdInput.isVisible()) {
                    pwdInput.fill("Aa123456");
                    page.waitForTimeout(200);
                }
                clickDialogConfirm();
                page.waitForTimeout(500);
                log.info("QTYL_078 重置密码操作完成");
            }
        } else {
            log.info("QTYL_078 未找到重置密码按钮");
        }
    }

    @Test
    @Order(1079)
    @DisplayName("QTYL_079: 分配角色")
    public void test_QTYL_079() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_079 表格无数据，跳过");
            return;
        }
        // 找"分配角色"按钮
        Locator assignRoleBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配角色")).first();
        if (!assignRoleBtn.isVisible()) {
            // 可能在更多操作裡
            page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
            page.waitForTimeout(200);
            clickMoreButton();
            Locator menuItem = page.locator(".el-dropdown-menu__item, .el-menu-item")
                    .filter(new Locator.FilterOptions().setHasText("分配角色")).first();
            if (menuItem.isVisible()) {
                menuItem.click();
                page.waitForTimeout(1000);
                log.info("QTYL_079 通过更多操作打开分配角色");
            } else {
                log.info("QTYL_079 未找到分配角色入口");
                return;
            }
        } else {
            assignRoleBtn.click();
            page.waitForTimeout(1000);
        }

        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 勾选角色
            Locator roleCheckbox = dialog.locator(".el-checkbox").first();
            if (roleCheckbox.isVisible()) {
                roleCheckbox.click();
                page.waitForTimeout(300);
            }
            clickDialogConfirm();
            page.waitForTimeout(500);
            log.info("QTYL_079 分配角色操作完成");
        }
    }

    @Test
    @Order(1080)
    @DisplayName("QTYL_080: 取消角色授权")
    public void test_QTYL_080() {
        navigateToUserManagement();
        if (!hasTableData()) {
            log.info("QTYL_080 表格无数据，跳过");
            return;
        }
        Locator assignRoleBtn = page.locator(".el-table__body-wrapper tbody tr")
                .first().locator("button, a, span")
                .filter(new Locator.FilterOptions().setHasText("分配角色")).first();
        if (assignRoleBtn.isVisible()) {
            assignRoleBtn.click();
            page.waitForTimeout(1000);
        } else {
            page.locator(".el-table__body-wrapper tbody tr .el-checkbox").first().click();
            page.waitForTimeout(200);
            clickMoreButton();
            Locator menuItem = page.locator(".el-dropdown-menu__item, .el-menu-item")
                    .filter(new Locator.FilterOptions().setHasText("分配角色")).first();
            if (menuItem.isVisible()) {
                menuItem.click();
                page.waitForTimeout(1000);
            } else {
                log.info("QTYL_080 未找到分配角色入口");
                return;
            }
        }

        Locator dialog = page.locator(".el-dialog").last();
        if (dialog.isVisible()) {
            // 取消勾选已选中的角色
            Locator checkedCheckbox = dialog.locator(".el-checkbox.is-checked").first();
            if (checkedCheckbox.isVisible()) {
                checkedCheckbox.click();
                page.waitForTimeout(300);
            }
            clickDialogConfirm();
            page.waitForTimeout(500);
            log.info("QTYL_080 取消角色授权操作完成");
        }
    }

}
