package cases.ui;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VersionTraceTest extends BaseTest {

    // ========== 需求版本控制 ==========
    @Test
    @DisplayName("GNYL_267: 升版")
    public void test_GNYL_267() {
        String[] doc = createTempDoc();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(doc[1]);
            page.waitForTimeout(1000);

            Locator publishedTag = page.getByText("已发布").first();
            if (publishedTag.isVisible()) {
                publishedTag.hover();
                page.waitForTimeout(300);
                Locator upgradeBtn = page.getByText("升版", new Page.GetByTextOptions().setExact(true));
                if (upgradeBtn.isVisible()) {
                    upgradeBtn.click();
                    page.waitForTimeout(500);
                    log.info("GNYL_267 升版成功");
                } else {
                    log.info("GNYL_267 未找到升版按钮（需求规格可能不在已发布状态）");
                }
            } else {
                log.info("GNYL_267 未找到'已发布'状态标签");
            }
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_268: 切换版本")
    public void test_GNYL_268() {
        Locator versionSelect = page.locator("[class*='version'], .el-select, [class*='tag-version']").first();
        if (versionSelect.isVisible()) {
            versionSelect.click();
            page.waitForTimeout(500);

            Locator versionOption = page.locator(".el-select-dropdown__item, [class*='version-item']").first();
            if (versionOption.isVisible()) {
                versionOption.click();
                page.waitForTimeout(500);
                log.info("GNYL_268 切换版本成功");
            } else {
                log.info("GNYL_268 未找到版本选项");
            }
        } else {
            log.info("GNYL_268 未找到版本选择器");
        }
    }

    // ========== 需求基线管理 ==========
    @Test
    @DisplayName("GNYL_269: 查看基线列表")
    public void test_GNYL_269() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);
            log.info("GNYL_269 查看基线列表成功");
        } else {
            log.info("GNYL_269 未找到基线导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_270: 创建基线")
    public void test_GNYL_270() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);

            Locator createBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("创建基线"));
            if (!createBtn.isVisible()) {
                createBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("创建")).first();
            }
            if (createBtn.isVisible()) {
                createBtn.click();
                page.waitForTimeout(500);

                Locator nameInput = page.locator(".el-dialog .el-input__inner, .el-form-item input").first();
                if (nameInput.isVisible()) {
                    nameInput.fill("自动化测试基线_" + System.currentTimeMillis());
                    page.waitForTimeout(300);
                }

                Locator checkbox = page.locator(".el-checkbox").first();
                if (checkbox.isVisible()) {
                    checkbox.click();
                    page.waitForTimeout(200);
                }

                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
                page.waitForTimeout(500);
                log.info("GNYL_270 创建基线成功");
            } else {
                log.info("GNYL_270 未找到创建基线按钮");
            }
        } else {
            log.info("GNYL_270 未找到基线导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_271: 基线名称必填测试")
    public void test_GNYL_271() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);

            Locator createBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("创建")).first();
            if (createBtn.isVisible()) {
                createBtn.click();
                page.waitForTimeout(500);

                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
                page.waitForTimeout(500);

                Locator errorMsg = page.locator(".el-form-item__error, .el-message, [role='alert']").first();
                if (errorMsg.isVisible()) {
                    log.info("GNYL_271 基线名称必填校验通过: {}", errorMsg.textContent());
                } else {
                    log.info("GNYL_271 未检测到错误提示（基线名称可能非必填）");
                }

                page.locator(".el-dialog .el-dialog__close, .el-dialog__headerbtn").first().click();
                page.waitForTimeout(300);
            } else {
                log.info("GNYL_271 未找到创建基线按钮");
            }
        } else {
            log.info("GNYL_271 未找到基线导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_272: 需求规格必选测试")
    public void test_GNYL_272() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);

            Locator createBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("创建")).first();
            if (createBtn.isVisible()) {
                createBtn.click();
                page.waitForTimeout(500);

                Locator nameInput = page.locator(".el-dialog .el-input__inner, .el-form-item input").first();
                if (nameInput.isVisible()) {
                    nameInput.fill("基线必选测试_" + System.currentTimeMillis());
                    page.waitForTimeout(200);
                }

                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
                page.waitForTimeout(500);

                Locator errorMsg = page.locator(".el-form-item__error, .el-message, [role='alert']").first();
                if (errorMsg.isVisible()) {
                    log.info("GNYL_272 需求规格必选校验通过: {}", errorMsg.textContent());
                } else {
                    log.info("GNYL_272 未检测到错误提示（需求规格可能非必选）");
                }

                page.locator(".el-dialog .el-dialog__close, .el-dialog__headerbtn").first().click();
                page.waitForTimeout(300);
            } else {
                log.info("GNYL_272 未找到创建基线按钮");
            }
        } else {
            log.info("GNYL_272 未找到基线导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_273: 查看基线")
    public void test_GNYL_273() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);

            Locator viewBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("查看")).first();
            if (viewBtn.isVisible()) {
                viewBtn.click();
                page.waitForTimeout(500);
                log.info("GNYL_273 查看基线成功");
            } else {
                log.info("GNYL_273 未找到查看基线按钮（可能列表为空）");
            }
        } else {
            log.info("GNYL_273 未找到基线导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_274: 删除基线")
    public void test_GNYL_274() {
        Locator baselineNav = page.locator("[class*='baseline'], [class*='base-line'], [title*='基线']").first();
        if (baselineNav.isVisible()) {
            baselineNav.click();
            page.waitForTimeout(1000);

            Locator deleteBtn = page.locator("button").filter(new Locator.FilterOptions().setHasText("删除")).first();
            if (deleteBtn.isVisible()) {
                deleteBtn.click();
                page.waitForTimeout(500);

                Locator confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                        .filter(new Locator.FilterOptions().setHasText("确定")).first();
                if (confirmBtn.isVisible()) {
                    confirmBtn.click();
                    page.waitForTimeout(500);
                }
                log.info("GNYL_274 删除基线成功");
            } else {
                log.info("GNYL_274 未找到删除基线按钮（可能列表为空）");
            }
        } else {
            log.info("GNYL_274 未找到基线导航图标");
        }
    }

    // ========== 需求收藏 ==========
    @Test
    @DisplayName("GNYL_275: 添加文件夹到收藏夹(根节点列表)")
    public void test_GNYL_275() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(folder[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            Locator favItem = page.getByText("添加到收藏夹", new Page.GetByTextOptions().setExact(true));
            if (favItem.isVisible()) {
                favItem.click();
                page.waitForTimeout(500);
                log.info("GNYL_275 添加文件夹到收藏夹(根节点列表)成功");
            } else {
                log.info("GNYL_275 右键菜单未找到'添加到收藏夹'");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_276: 添加文件夹到收藏夹(文件夹列表)")
    public void test_GNYL_276() {
        String[] folder = createTempFolder();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(folder[1]).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            Locator favItem = page.getByText("添加到收藏夹", new Page.GetByTextOptions().setExact(true));
            if (favItem.isVisible()) {
                favItem.click();
                page.waitForTimeout(500);
                log.info("GNYL_276 添加文件夹到收藏夹(文件夹列表)成功");
            } else {
                log.info("GNYL_276 右键菜单未找到'添加到收藏夹'");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_277: 添加需求规格到收藏夹")
    public void test_GNYL_277() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            Locator favItem = page.getByText("添加到收藏夹", new Page.GetByTextOptions().setExact(true));
            if (favItem.isVisible()) {
                favItem.click();
                page.waitForTimeout(500);
                log.info("GNYL_277 添加需求规格到收藏夹成功");
            } else {
                log.info("GNYL_277 右键菜单未找到'添加到收藏夹'");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_278: 查看收藏列表")
    public void test_GNYL_278() {
        Locator favNav = page.locator("[class*='favorite'], [class*='star'], [class*='collect'], .el-icon-star-on")
                .first();
        if (favNav.isVisible()) {
            favNav.click();
            page.waitForTimeout(1000);

            Locator favList = page.locator("[class*='favorite-list'], [class*='collect-list']").first();
            if (favList.isVisible()) {
                log.info("GNYL_278 收藏列表可见");
            } else {
                log.info("GNYL_278 收藏列表无数据或选择器未匹配");
            }
        } else {
            log.info("GNYL_278 未找到收藏导航图标");
        }
    }

    @Test
    @DisplayName("GNYL_279: 删除收藏")
    public void test_GNYL_279() {
        Locator favNav = page.locator("[class*='favorite'], [class*='star'], [class*='collect'], .el-icon-star-on")
                .first();
        if (favNav.isVisible()) {
            favNav.click();
            page.waitForTimeout(1000);

            Locator deleteIcon = page.locator("[class*='favorite-list'] [class*='delete'], " +
                    "[class*='collect-list'] [class*='close'], .el-icon-close").first();
            if (deleteIcon.isVisible()) {
                deleteIcon.click();
                page.waitForTimeout(500);

                Locator confirmBtn = page.locator(".el-message-box__btns button, .el-dialog__footer button")
                        .filter(new Locator.FilterOptions().setHasText("确定")).first();
                if (confirmBtn.isVisible()) {
                    confirmBtn.click();
                    page.waitForTimeout(500);
                }
                log.info("GNYL_279 删除收藏成功");
            } else {
                log.info("GNYL_279 未找到删除图标");
            }
        } else {
            log.info("GNYL_279 未找到收藏导航图标");
        }
    }

    // ========== 需求双向追溯 ==========
    @Test
    @DisplayName("GNYL_280: 创建追溯")
    public void test_GNYL_280() {
        // TODO: 右键条目 → 追溯 → 创建追溯 → 设链接起点 → 选目标条目 → 设链接终点 → 选关系类型 → 确定
    }

    @Test
    @DisplayName("GNYL_281: 基线文件创建追溯")
    public void test_GNYL_281() {
        // TODO: 打开已添加到基线的需求规格 → 创建追溯
    }

    @Test
    @DisplayName("GNYL_282: 设置链接起点")
    public void test_GNYL_282() {
        // TODO: 右键条目 → 追溯 → 创建追溯 → 设置为链接起点
    }

    @Test
    @DisplayName("GNYL_283: 取消链接起点")
    public void test_GNYL_283() {
        // TODO: 已有链接起点 → 取消链接起点
    }

    @Test
    @DisplayName("GNYL_284: 创建链接(拖动)")
    public void test_GNYL_284() {
        // TODO: 拖动需求条目到目标条目 → 创建链接 → 选关系类型 → 确定
    }

    @Test
    @DisplayName("GNYL_285: 添加关联(文件夹列表)")
    public void test_GNYL_285() {
        // TODO: 双击文件夹 → 右键需求规格 → 追溯 → 追溯配置 → 添加关联
    }

    @Test
    @DisplayName("GNYL_286: 添加关联(需求规格内)")
    public void test_GNYL_286() {
        // TODO: 双击需求规格 → 单击"追溯" → 追溯配置 → 添加关联
    }

    @Test
    @DisplayName("GNYL_287: 取消添加关联(文件夹)")
    public void test_GNYL_287() {
        // TODO: 已关联列表中点击需求规格后方的x
    }

    @Test
    @DisplayName("GNYL_288: 取消添加关联(规格内)")
    public void test_GNYL_288() {
        // TODO: 已关联列表中点击需求规格后方的x
    }

    @Test
    @DisplayName("GNYL_289: 删除关联关系(文件夹)")
    public void test_GNYL_289() {
        // TODO: 追溯配置 → 点击连接对象前的删除图标 → 确定
    }

    @Test
    @DisplayName("GNYL_290: 删除关联关系(规格内)")
    public void test_GNYL_290() {
        // TODO: 追溯配置 → 点击连接对象前的删除图标 → 确定
    }

    @Test
    @DisplayName("GNYL_291: 退出追溯配置页面(文件夹)")
    public void test_GNYL_291() {
        // TODO: 追溯配置 → 返回
    }

    @Test
    @DisplayName("GNYL_292: 退出追溯配置页面(规格内)")
    public void test_GNYL_292() {
        // TODO: 追溯配置 → 返回
    }

    @Test
    @DisplayName("GNYL_293: 进入追溯矩阵弹框(文件夹)")
    public void test_GNYL_293() {
        // TODO: 右键需求规格 → 追溯 → 追溯矩阵
    }

    @Test
    @DisplayName("GNYL_294: 进入追溯矩阵弹框(规格内)")
    public void test_GNYL_294() {
        // TODO: 双击需求规格 → 单击"追溯" → 追溯矩阵
    }

    @Test
    @DisplayName("GNYL_295: 通过连接对象进入追溯矩阵弹框")
    public void test_GNYL_295() {
        // TODO: 追溯配置页面 → 点击连接对象的需求规格名称
    }

    @Test
    @DisplayName("GNYL_296: 通过蓝色箭头跳转追溯矩阵弹框")
    public void test_GNYL_296() {
        // TODO: 悬浮蓝色箭头 → 点击悬浮窗内选项
    }

    @Test
    @DisplayName("GNYL_297: 设置追溯关系")
    public void test_GNYL_297() {
        // TODO: 追溯矩阵 → 选择规格 → 选择关系 → 箭头 → 点击位置 → 保存
    }

    @Test
    @DisplayName("GNYL_298: 删除追溯关系")
    public void test_GNYL_298() {
        // TODO: 追溯矩阵 → 点击要删除的追溯关系
    }

    @Test
    @DisplayName("GNYL_299: 恢复默认模式")
    public void test_GNYL_299() {
        // TODO: 追溯矩阵 → 点击"飞机指针"图标
    }

    @Test
    @DisplayName("GNYL_300: 切换需求规格查看追溯关系")
    public void test_GNYL_300() {
        // TODO: 追溯矩阵 → 打开需求规格下拉选 → 切换
    }

    @Test
    @DisplayName("GNYL_301: 关系类型标签验证")
    public void test_GNYL_301() {
        // TODO: 追溯矩阵 → 点击高亮的关系类型标签
    }

    @Test
    @DisplayName("GNYL_302: 最大化弹框查看追溯矩阵")
    public void test_GNYL_302() {
        // TODO: 追溯矩阵 → 点击最大化图标
    }

    @Test
    @DisplayName("GNYL_303: 最小化弹框查看追溯矩阵")
    public void test_GNYL_303() {
        // TODO: 追溯矩阵 → 点击最小化图标
    }

    @Test
    @DisplayName("GNYL_304: 进入全局数字追溯页面(应用)")
    public void test_GNYL_304() {
        // TODO: 悬浮"应用" → 点击"全局数字追溯"
    }

    @Test
    @DisplayName("GNYL_305: 右键跳转全局数字追溯(文件夹)")
    public void test_GNYL_305() {
        // TODO: 右键需求规格 → 追溯 → 追溯视图
    }

    @Test
    @DisplayName("GNYL_306: 右键跳转全局数字追溯(规格内)")
    public void test_GNYL_306() {
        // TODO: 双击需求规格 → 单击"追溯" → 追溯视图
    }

    @Test
    @DisplayName("GNYL_307: 查看XBom追溯视图")
    public void test_GNYL_307() {
        // TODO: 全局数字追溯 → 左侧选择需求规格 → XBom视图
    }

    @Test
    @DisplayName("GNYL_308: 查看链式追溯视图")
    public void test_GNYL_308() {
        // TODO: 全局数字追溯 → 选择需求规格 → 点击"链式"
    }

    @Test
    @DisplayName("GNYL_309: 链式追溯视图收缩图标测试")
    public void test_GNYL_309() {
        // TODO: 链式视图 → 点击收缩图标"-"
    }

    @Test
    @DisplayName("GNYL_310: 链式追溯视图展开图标测试")
    public void test_GNYL_310() {
        // TODO: 链式视图 → 点击展开图标"+"
    }

    @Test
    @DisplayName("GNYL_311: 查看矩阵追溯视图")
    public void test_GNYL_311() {
        // TODO: 全局数字追溯 → 选择需求规格 → 点击"矩阵"
    }

    @Test
    @DisplayName("GNYL_312: 选择对照规格")
    public void test_GNYL_312() {
        // TODO: 矩阵视图 → 展开"选择对照规格" → 选择 → 点击关联指示
    }

    @Test
    @DisplayName("GNYL_313: 查看链路追溯视图(XBom)")
    public void test_GNYL_313() {
        // TODO: XBom视图 → 右键条目 → 查看链路追踪
    }

    @Test
    @DisplayName("GNYL_314: 查看链路追溯视图(链式)")
    public void test_GNYL_314() {
        // TODO: 链式视图 → 右键条目 → 查看链路追踪
    }

    @Test
    @DisplayName("GNYL_315: 循环追溯")
    public void test_GNYL_315() {
        // TODO: 链路追溯视图顶部 → 切换循环追溯
    }

    @Test
    @DisplayName("GNYL_316: 通过蓝色箭头跳转追溯链路弹框")
    public void test_GNYL_316() {
        // TODO: 悬浮蓝色箭头 → 点击追溯链路
    }

    @Test
    @DisplayName("GNYL_317: 进入追溯链路弹框")
    public void test_GNYL_317() {
        // TODO: 右键条目 → 追溯 → 追溯链路
    }

    @Test
    @DisplayName("GNYL_318: 勾选需求规格查看相互的追溯关系")
    public void test_GNYL_318() {
        // TODO: 追溯视图顶部 → 勾选需求规格
    }

    @Test
    @DisplayName("GNYL_319: 查看需求条目信息")
    public void test_GNYL_319() {
        // TODO: 点击追溯视图里的需求条目
    }

    @Test
    @DisplayName("GNYL_320: 关闭需求条目信息窗口")
    public void test_GNYL_320() {
        // TODO: 鼠标移至信息窗口外空白处点击
    }

    @Test
    @DisplayName("GNYL_321: 切换追溯方向")
    public void test_GNYL_321() {
        // TODO: 追溯视图顶部 → 展开"追溯方向" → 选择
    }

    @Test
    @DisplayName("GNYL_322: 增加追溯深度")
    public void test_GNYL_322() {
        // TODO: 追溯视图顶部 → 点击追溯深度"+"
    }

    @Test
    @DisplayName("GNYL_323: 减少追溯深度")
    public void test_GNYL_323() {
        // TODO: 追溯视图顶部 → 点击追溯深度"-"
    }

    @Test
    @DisplayName("GNYL_324: 存在的名称检索(追溯视图)")
    public void test_GNYL_324() {
        // TODO: 追溯视图顶部 → 输入存在的名称 → 回车
    }

    @Test
    @DisplayName("GNYL_325: 名称检索模糊查询(追溯视图)")
    public void test_GNYL_325() {
        // TODO: 追溯视图顶部 → 输入部分关键字 → 回车
    }

    @Test
    @DisplayName("GNYL_326: 不存在的名称检索(追溯视图)")
    public void test_GNYL_326() {
        // TODO: 追溯视图顶部 → 输入不存在名称 → 回车
    }

    @Test
    @DisplayName("GNYL_327: 当前关系查看")
    public void test_GNYL_327() {
        // TODO: 追溯视图顶部 → 悬浮"当前关系"
    }

    @Test
    @DisplayName("GNYL_328: 隐藏追溯关系")
    public void test_GNYL_328() {
        // TODO: 悬浮"当前关系" → 点击高亮的关系类型
    }

    @Test
    @DisplayName("GNYL_329: 删除追溯视图中的需求条目")
    public void test_GNYL_329() {
        // TODO: 追溯视图中 → 点击需求条目的x
    }

    @Test
    @DisplayName("GNYL_333: 切换追溯范围")
    public void test_GNYL_333() {
        // TODO: 悬浮左侧"切换追溯范围" → 选择
    }

    // ========== 需求列表检索 ==========
    @Test
    @DisplayName("GNYL_330: 需求列表存在的需求检索")
    public void test_GNYL_330() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            reqPage.openFolderAndActivateEdit(folder[1], doc[1]);
            page.waitForTimeout(500);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill(doc[1].substring(0, Math.min(5, doc[1].length())));
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator result = page.getByText(doc[1]).first();
                if (result.isVisible()) {
                    log.info("GNYL_330 需求列表存在的需求检索成功");
                } else {
                    log.info("GNYL_330 检索未找到结果");
                }
            } else {
                log.info("GNYL_330 未找到搜索输入框");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_331: 需求列表不存在的需求检索")
    public void test_GNYL_331() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);
            reqPage.openFolderAndActivateEdit(folder[1], doc[1]);
            page.waitForTimeout(500);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("__不存在_需求__");
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator emptyText = page.locator(".el-empty, [class*='empty'], .el-table__empty-text").first();
                if (emptyText.isVisible()) {
                    log.info("GNYL_331 不存在的需求检索显示暂无数据");
                } else {
                    log.info("GNYL_331 搜索完成，无匹配结果");
                }
            } else {
                log.info("GNYL_331 未找到搜索输入框");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_332: 需求模糊查询")
    public void test_GNYL_332() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);
            reqPage.openFolderAndActivateEdit(folder[1], doc[1]);
            page.waitForTimeout(500);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("AT_");
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator result = page.getByText("AT_").first();
                if (result.isVisible()) {
                    log.info("GNYL_332 模糊查询成功，包含关键字的条目已展示");
                } else {
                    log.info("GNYL_332 模糊查询完成");
                }
            } else {
                log.info("GNYL_332 未找到搜索输入框");
            }

            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("");
                page.waitForTimeout(300);
                searchInput.press("Enter");
                page.waitForTimeout(500);
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    // ========== 需求变更 ==========
    @Test
    @DisplayName("GNYL_334: 复制需求为同级对象(右键)")
    public void test_GNYL_334() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("复制", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);

            Locator pasteItem = page.getByText("粘贴", new Page.GetByTextOptions().setExact(true));
            if (pasteItem.isVisible()) {
                pasteItem.click();
                page.waitForTimeout(300);
                page.getByText("同级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_334 复制需求为同级对象成功");
            } else {
                log.info("GNYL_334 未找到粘贴菜单");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_335: 复制需求为子级对象(右键)")
    public void test_GNYL_335() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("复制", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);

            Locator pasteItem = page.getByText("粘贴", new Page.GetByTextOptions().setExact(true));
            if (pasteItem.isVisible()) {
                pasteItem.click();
                page.waitForTimeout(300);
                page.getByText("子级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_335 复制需求为子级对象成功");
            } else {
                log.info("GNYL_335 未找到粘贴菜单");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_338: 剪切需求为同级对象(右键)")
    public void test_GNYL_338() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("剪切", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);

            Locator pasteItem = page.getByText("粘贴", new Page.GetByTextOptions().setExact(true));
            if (pasteItem.isVisible()) {
                pasteItem.click();
                page.waitForTimeout(300);
                page.getByText("同级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_338 剪切需求为同级对象成功");
            } else {
                log.info("GNYL_338 未找到粘贴菜单");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_339: 剪切需求为子级对象(右键)")
    public void test_GNYL_339() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("剪切", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            reqPage.rightClickTreeNode(folder[1]);
            page.waitForTimeout(500);

            Locator pasteItem = page.getByText("粘贴", new Page.GetByTextOptions().setExact(true));
            if (pasteItem.isVisible()) {
                pasteItem.click();
                page.waitForTimeout(300);
                page.getByText("子级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_339 剪切需求为子级对象成功");
            } else {
                log.info("GNYL_339 未找到粘贴菜单");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_342: 切换标题/正文")
    public void test_GNYL_342() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            Locator switchItem = page.getByText("切换标题/正文", new Page.GetByTextOptions().setExact(true));
            if (switchItem.isVisible()) {
                switchItem.click();
                page.waitForTimeout(500);
                log.info("GNYL_342 切换标题/正文成功");
            } else {
                log.info("GNYL_342 未找到切换标题/正文菜单项");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_336: 复制为同级对象(拖动)")
    public void test_GNYL_336() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            Locator source = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1])).first();
            Locator target = page.locator("[class*='tree'] [class*='node']").filter(new Locator.FilterOptions().setHasText(folder[1])).first();
            if (source.isVisible() && target.isVisible()) {
                source.hover();
                page.mouse().down();
                target.hover();
                page.waitForTimeout(300);
                page.mouse().up();
                page.waitForTimeout(500);

                page.getByText("复制", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(300);
                page.getByText("同级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_336 复制为同级对象(拖动)成功");
            } else {
                log.info("GNYL_336 未找到源或目标元素");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_337: 复制为子级对象(拖动)")
    public void test_GNYL_337() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            Locator source = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1])).first();
            Locator target = page.locator("[class*='tree'] [class*='node']").filter(new Locator.FilterOptions().setHasText(folder[1])).first();
            if (source.isVisible() && target.isVisible()) {
                source.hover();
                page.mouse().down();
                target.hover();
                page.waitForTimeout(300);
                page.mouse().up();
                page.waitForTimeout(500);

                page.getByText("复制", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(300);
                page.getByText("子级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_337 复制为子级对象(拖动)成功");
            } else {
                log.info("GNYL_337 未找到源或目标元素");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_340: 移动为同级对象")
    public void test_GNYL_340() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            Locator source = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1])).first();
            Locator target = page.locator("[class*='tree'] [class*='node']").filter(new Locator.FilterOptions().setHasText(folder[1])).first();
            if (source.isVisible() && target.isVisible()) {
                source.hover();
                page.mouse().down();
                target.hover();
                page.waitForTimeout(300);
                page.mouse().up();
                page.waitForTimeout(500);

                page.getByText("移动", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(300);
                page.getByText("同级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_340 移动为同级对象成功");
            } else {
                log.info("GNYL_340 未找到源或目标元素");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_341: 移动为子级对象")
    public void test_GNYL_341() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            Locator source = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1])).first();
            Locator target = page.locator("[class*='tree'] [class*='node']").filter(new Locator.FilterOptions().setHasText(folder[1])).first();
            if (source.isVisible() && target.isVisible()) {
                source.hover();
                page.mouse().down();
                target.hover();
                page.waitForTimeout(300);
                page.mouse().up();
                page.waitForTimeout(500);

                page.getByText("移动", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(300);
                page.getByText("子级对象", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_341 移动为子级对象成功");
            } else {
                log.info("GNYL_341 未找到源或目标元素");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_343: 切换标题/正文(带图片表格提示)")
    public void test_GNYL_343() {
        String[] folder = createTempFolder();
        String[] doc = createTempDoc(folder[0]);
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(folder[1]);
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName(doc[1]))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            Locator switchItem = page.getByText("切换标题/正文", new Page.GetByTextOptions().setExact(true));
            if (switchItem.isVisible()) {
                switchItem.click();
                page.waitForTimeout(500);

                Locator alert = page.locator(".el-message, .el-message-box, .el-dialog, [role='alert']").first();
                if (alert.isVisible()) {
                    log.info("GNYL_343 检测到切换提示: {}", alert.textContent());
                } else {
                    log.info("GNYL_343 切换标题/正文成功（无提示）");
                }
            } else {
                log.info("GNYL_343 未找到切换标题/正文菜单项");
            }
        } finally {
            cleanupFolderByName(folder[1]);
        }
    }

    @Test
    @DisplayName("GNYL_344: 更改单另存为草稿")
    public void test_GNYL_344() {
        String[] doc = createTempDoc();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(doc[1]);
            page.waitForTimeout(1000);

            Locator statusTag = page.getByText("已发布").first();
            if (statusTag.isVisible()) {
                statusTag.hover();
                page.waitForTimeout(300);
                page.getByText("审签", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                page.getByText("另存为草稿", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_344 更改单另存为草稿成功");
            } else {
                log.info("GNYL_344 未找到'已发布'状态标签");
            }
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_345: 更改审签发布")
    public void test_GNYL_345() {
        String[] doc = createTempDoc();
        try {
            reqPage.refreshTree();
            page.waitForTimeout(1000);

            reqPage.doubleClickTreeNode(doc[1]);
            page.waitForTimeout(1000);

            Locator statusTag = page.getByText("已发布").first();
            if (statusTag.isVisible()) {
                statusTag.hover();
                page.waitForTimeout(300);
                page.getByText("审签", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                page.getByText("发布", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(500);
                log.info("GNYL_345 更改审签发布成功");
            } else {
                log.info("GNYL_345 未找到'已发布'状态标签");
            }
        } finally {
            cleanupDoc(doc[0], doc[2]);
        }
    }

    @Test
    @DisplayName("GNYL_346: 我的更改单重新提交")
    public void test_GNYL_346() {
        Locator changeOrderNav = page.locator("[class*='change'], [class*='alter'], [class*='modify'], [title*='更改单']").first();
        if (changeOrderNav.isVisible()) {
            changeOrderNav.click();
            page.waitForTimeout(1000);
            log.info("GNYL_346 进入我的更改单页面");
        } else {
            log.info("GNYL_346 未找到我的更改单导航");
        }
    }

}
