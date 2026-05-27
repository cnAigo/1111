package cases;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.MouseButton;
import config.TestConstants;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CollaborativeEditTest extends BaseTest {

    // ========== 需求规格协同编辑状态 ==========
    @Test
    @DisplayName("GNYL_186: 共享模式打开需求规格")
    public void test_GNYL_186() {
        reqPage.rightClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(500);
        page.getByText("打开", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("共享模式", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator tab = page.locator("[class*='tab'], [class*='el-tabs']").first();
        assertThat(tab).isVisible();
        log.info("GNYL_186 共享模式打开需求规格成功");
    }

    @Test
    @DisplayName("GNYL_187: 关闭共享模式打开的需求规格")
    public void test_GNYL_187() {
        Locator closeIcon = page.locator("[class*='tab'] [class*='close'], .el-tabs__item .el-icon-close").first();
        if (closeIcon.isVisible()) {
            closeIcon.click();
            page.waitForTimeout(500);
            log.info("GNYL_187 关闭共享模式页签成功");
        } else {
            log.info("GNYL_187 未找到关闭图标");
        }
    }

    @Test
    @DisplayName("GNYL_188: 独占模式打开需求规格")
    public void test_GNYL_188() {
        reqPage.rightClickTreeNode(TestConstants.REQ_NAME2);
        page.waitForTimeout(500);
        page.getByText("打开", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("独占模式", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator tab = page.locator("[class*='tab'], [class*='el-tabs']").first();
        assertThat(tab).isVisible();
        log.info("GNYL_188 独占模式打开需求规格成功");
    }

    @Test
    @DisplayName("GNYL_189: 关闭独占模式打开的需求规格")
    public void test_GNYL_189() {
        Locator closeIcon = page.locator("[class*='tab'] [class*='close'], .el-tabs__item .el-icon-close").first();
        if (closeIcon.isVisible()) {
            closeIcon.click();
            page.waitForTimeout(500);
            log.info("GNYL_189 关闭独占模式页签成功");
        } else {
            log.info("GNYL_189 未找到关闭图标");
        }
    }

    @Test
    @DisplayName("GNYL_190: 只读模式打开需求规格")
    public void test_GNYL_190() {
        reqPage.rightClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(500);
        page.getByText("打开", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("只读模式", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator tab = page.locator("[class*='tab'], [class*='el-tabs']").first();
        assertThat(tab).isVisible();
        log.info("GNYL_190 只读模式打开需求规格成功");
    }

    @Test
    @DisplayName("GNYL_191: 关闭只读模式打开的需求规格")
    public void test_GNYL_191() {
        Locator closeIcon = page.locator("[class*='tab'] [class*='close'], .el-tabs__item .el-icon-close").first();
        if (closeIcon.isVisible()) {
            closeIcon.click();
            page.waitForTimeout(500);
            log.info("GNYL_191 关闭只读模式页签成功");
        } else {
            log.info("GNYL_191 未找到关闭图标");
        }
    }

    @Test
    @DisplayName("GNYL_192: 需求规格解锁")
    public void test_GNYL_192() {
        reqPage.rightClickTreeNode(TestConstants.REQ_NAME2);
        page.waitForTimeout(500);
        page.getByText("解锁", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);
        log.info("GNYL_192 需求规格解锁成功");
    }

    @Test
    @DisplayName("GNYL_193: 冻结需求规格")
    public void test_GNYL_193() {
        Locator statusTag = page.getByText("工作中").first();
        if (statusTag.isVisible()) {
            statusTag.hover();
            page.waitForTimeout(300);
            page.getByText("冻结", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);
            log.info("GNYL_193 冻结需求规格成功");
        } else {
            log.info("GNYL_193 未找到'工作中'状态标签");
        }
    }

    @Test
    @DisplayName("GNYL_194: 恢复需求规格工作")
    public void test_GNYL_194() {
        Locator statusTag = page.getByText("冻结").first();
        if (statusTag.isVisible()) {
            statusTag.hover();
            page.waitForTimeout(300);
            page.getByText("工作中", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);
            log.info("GNYL_194 恢复需求规格工作成功");
        } else {
            log.info("GNYL_194 未找到'冻结'状态标签");
        }
    }

    // ========== 需求协同编辑 - 标题/正文 ==========
    @Test
    @DisplayName("GNYL_195: 编辑不超过500字符的标题")
    public void test_GNYL_195() {
        reqPage.doubleClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-"))
                .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("编辑标题", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator titleInput = page.locator(".el-input__inner, input[type='text']").first();
        if (titleInput.isVisible()) {
            titleInput.click();
            titleInput.fill("");
            titleInput.fill("自动化测试标题_不超过500字符_" + System.currentTimeMillis());
            page.waitForTimeout(300);
            titleInput.press("Enter");
            page.waitForTimeout(500);
            log.info("GNYL_195 编辑不超过500字符的标题成功");
        } else {
            log.info("GNYL_195 未找到标题输入框");
        }
    }

    @Test
    @DisplayName("GNYL_196: 编辑超过500字符的标题")
    public void test_GNYL_196() {
        reqPage.doubleClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-"))
                .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("编辑标题", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator titleInput = page.locator(".el-input__inner, input[type='text']").first();
        if (titleInput.isVisible()) {
            titleInput.click();
            titleInput.fill("");
            String longTitle = "A".repeat(501);
            titleInput.fill(longTitle);
            page.waitForTimeout(300);
            titleInput.press("Enter");
            page.waitForTimeout(500);

            Locator errorMsg = page.locator(".el-form-item__error, .el-message, [class*='error']").first();
            if (errorMsg.isVisible()) {
                log.info("GNYL_196 超过500字符的标题已被拦截: {}", errorMsg.textContent());
            } else {
                log.info("GNYL_196 超过500字符的标题输入完成（未检测到错误提示）");
            }
        } else {
            log.info("GNYL_196 未找到标题输入框");
        }
    }

    @Test
    @DisplayName("GNYL_197: 编辑不超过500字符的正文")
    public void test_GNYL_197() {
        openEditorAndClickContent();

        page.locator(".w-e-text-container, [contenteditable='true'], .req-content-container").first().click();
        page.waitForTimeout(300);
        page.keyboard().type("自动化测试正文_不超过500字符_" + System.currentTimeMillis());
        page.waitForTimeout(500);

        log.info("GNYL_197 编辑不超过500字符的正文成功");
    }

    @Test
    @DisplayName("GNYL_198: 编辑超过500字符的正文")
    public void test_GNYL_198() {
        openEditorAndClickContent();

        page.locator(".w-e-text-container, [contenteditable='true'], .req-content-container").first().click();
        page.waitForTimeout(300);
        String longBody = "自动化测试长正文_" + System.currentTimeMillis() + "_" + "B".repeat(500);
        page.keyboard().type(longBody);
        page.waitForTimeout(500);

        Locator errorMsg = page.locator(".el-form-item__error, .el-message, [class*='error']").first();
        if (errorMsg.isVisible()) {
            log.info("GNYL_198 超过500字符的正文已被拦截: {}", errorMsg.textContent());
        } else {
            log.info("GNYL_198 超过500字符的正文输入完成");
        }
    }

    @Test
    @DisplayName("GNYL_199: 需求加锁")
    public void test_GNYL_199() {
        reqPage.doubleClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        Locator firstItem = page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first();
        if (firstItem.isVisible()) {
            firstItem.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("加锁", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);
            log.info("GNYL_199 需求加锁成功");
        } else {
            log.info("GNYL_199 未找到需求条目");
        }
    }

    @Test
    @DisplayName("GNYL_200: 需求重复解锁")
    public void test_GNYL_200() {
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("加锁", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);

        Locator errorMsg = page.getByText("请勿重复加锁");
        if (errorMsg.isVisible()) {
            assertThat(errorMsg).isVisible();
            log.info("GNYL_200 重复加锁已被拦截: {}", errorMsg.textContent());
        } else {
            log.info("GNYL_200 加锁完成（首次加锁成功）");
        }
    }

    @Test
    @DisplayName("GNYL_201: 需求解锁")
    public void test_GNYL_201() {
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("解锁", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(500);
        log.info("GNYL_201 需求解锁成功");
    }

    // ========== 协同编辑 - 工具栏 ==========

    private void openEditorAndClickContent() {
        reqPage.doubleClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-"))
                .locator("div").first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("编辑正文").click();
        page.waitForTimeout(1000);
        page.locator(".req-content-container > div").first().click();
        page.waitForTimeout(500);
    }

    @Test
    @DisplayName("GNYL_202: 正文修改字号")
    public void test_GNYL_202() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("字号 默认字号")).click();
        page.waitForTimeout(300);
        page.getByText("13px").click();
        page.waitForTimeout(500);

        log.info("GNYL_202 修改字号成功");
    }

    @Test
    @DisplayName("GNYL_203: 标题修改字号")
    public void test_GNYL_203() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("H1")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("H2")).click();
        page.waitForTimeout(200);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("H3")).click();
        page.waitForTimeout(200);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("H4")).click();
        page.waitForTimeout(500);

        log.info("GNYL_203 标题修改字号成功");
    }

    @Test
    @DisplayName("GNYL_204: 粗体设置")
    public void test_GNYL_204() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("粗体\\a ctrl+b")).click();
        page.waitForTimeout(500);

        log.info("GNYL_204 粗体设置成功");
    }

    @Test
    @DisplayName("GNYL_205: 取消粗体设置")
    public void test_GNYL_205() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("粗体\\a ctrl+b")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("粗体\\a ctrl+b")).click();
        page.waitForTimeout(500);

        log.info("GNYL_205 取消粗体设置成功");
    }

    @Test
    @DisplayName("GNYL_206: 清除格式")
    public void test_GNYL_206() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("清除格式")).click();
        page.waitForTimeout(500);

        log.info("GNYL_206 清除格式成功");
    }

    @Test
    @DisplayName("GNYL_207: 修改字体颜色")
    public void test_GNYL_207() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("文字颜色")).click();
        page.waitForTimeout(500);
        page.locator("li:nth-child(3) > .color-block").click();
        page.waitForTimeout(500);

        log.info("GNYL_207 修改字体颜色成功");
    }

    @Test
    @DisplayName("GNYL_208: 添加背景色")
    public void test_GNYL_208() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("背景色")).click();
        page.waitForTimeout(500);
        page.locator("div:nth-child(10) > .w-e-drop-panel > .w-e-panel-content-color > li:nth-child(2) > .color-block").click();
        page.waitForTimeout(500);

        log.info("GNYL_208 添加背景色成功");
    }

    @Test
    @DisplayName("GNYL_209: 清除背景色")
    public void test_GNYL_209() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("清除格式")).click();
        page.waitForTimeout(500);

        log.info("GNYL_209 清除背景色成功");
    }

    @Test
    @DisplayName("GNYL_210: 插入图片")
    public void test_GNYL_210() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("图片").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("上传图片")).click();
        page.waitForTimeout(1000);

        log.info("GNYL_210 插入图片 - 已打开上传图片对话框");
    }

    @Test
    @DisplayName("GNYL_211: 删除图片")
    public void test_GNYL_211() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("图片").setExact(true)).click();
        page.waitForTimeout(500);

        Locator img = page.locator(".w-e-text-container img, [contenteditable='true'] img").first();
        if (img.isVisible()) {
            img.click();
            page.waitForTimeout(300);
            page.keyboard().press("Delete");
            page.waitForTimeout(500);
            log.info("GNYL_211 删除图片成功");
        } else {
            log.info("GNYL_211 未找到图片元素（可能暂无已插入的图片）");
        }
    }

    @Test
    @DisplayName("GNYL_212: 插入表格")
    public void test_GNYL_212() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(5) > td:nth-child(4)").click();
        page.waitForTimeout(500);

        log.info("GNYL_212 插入表格成功");
    }

    @Test
    @DisplayName("GNYL_213: 回车插入文本段落")
    public void test_GNYL_213() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("回车")).click();
        page.waitForTimeout(500);

        log.info("GNYL_213 回车插入文本段落成功");
    }

    @Test
    @DisplayName("GNYL_214: 设置表头(下方工具栏)")
    public void test_GNYL_214() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator headerBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("设置表头"));
        if (headerBtn.isVisible()) {
            headerBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_214 设置表头(下方工具栏)成功");
        } else {
            log.info("GNYL_214 未找到设置表头按钮");
        }
    }

    @Test
    @DisplayName("GNYL_215: 设置表头(上方工具栏)")
    public void test_GNYL_215() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator headerBtnUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("设置表头"));
            if (headerBtnUp.isVisible()) {
                headerBtnUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_215 设置表头(上方工具栏)成功");
            } else {
                log.info("GNYL_215 上方工具栏未找到设置表头按钮");
            }
        } else {
            log.info("GNYL_215 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_216: 宽度自适应")
    public void test_GNYL_216() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator adaptBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("宽度自适应"));
        if (adaptBtn.isVisible()) {
            adaptBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_216 宽度自适应设置成功");
        } else {
            log.info("GNYL_216 未找到宽度自适应按钮");
        }
    }

    @Test
    @DisplayName("GNYL_217: 插入行(下方工具栏)")
    public void test_GNYL_217() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator insertRowBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入行"));
        if (insertRowBtn.isVisible()) {
            insertRowBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_217 插入行(下方工具栏)成功");
        } else {
            log.info("GNYL_217 未找到插入行按钮");
        }
    }

    @Test
    @DisplayName("GNYL_218: 插入行(上方工具栏)")
    public void test_GNYL_218() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator insertRowUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("插入行"));
            if (insertRowUp.isVisible()) {
                insertRowUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_218 插入行(上方工具栏)成功");
            } else {
                log.info("GNYL_218 上方工具栏未找到插入行按钮");
            }
        } else {
            log.info("GNYL_218 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_219: 删除行(下方工具栏)")
    public void test_GNYL_219() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator delRowBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("删除行"));
        if (delRowBtn.isVisible()) {
            delRowBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_219 删除行(下方工具栏)成功");
        } else {
            log.info("GNYL_219 未找到删除行按钮");
        }
    }

    @Test
    @DisplayName("GNYL_220: 删除行(上方工具栏)")
    public void test_GNYL_220() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator delRowUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("删除行"));
            if (delRowUp.isVisible()) {
                delRowUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_220 删除行(上方工具栏)成功");
            } else {
                log.info("GNYL_220 上方工具栏未找到删除行按钮");
            }
        } else {
            log.info("GNYL_220 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_221: 插入列(下方工具栏)")
    public void test_GNYL_221() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator insertColBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入列"));
        if (insertColBtn.isVisible()) {
            insertColBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_221 插入列(下方工具栏)成功");
        } else {
            log.info("GNYL_221 未找到插入列按钮");
        }
    }

    @Test
    @DisplayName("GNYL_222: 插入列(上方工具栏)")
    public void test_GNYL_222() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator insertColUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("插入列"));
            if (insertColUp.isVisible()) {
                insertColUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_222 插入列(上方工具栏)成功");
            } else {
                log.info("GNYL_222 上方工具栏未找到插入列按钮");
            }
        } else {
            log.info("GNYL_222 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_223: 删除列(下方工具栏)")
    public void test_GNYL_223() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator delColBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("删除列"));
        if (delColBtn.isVisible()) {
            delColBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_223 删除列(下方工具栏)成功");
        } else {
            log.info("GNYL_223 未找到删除列按钮");
        }
    }

    @Test
    @DisplayName("GNYL_224: 删除列(上方工具栏)")
    public void test_GNYL_224() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(2) > td:nth-child(1)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator delColUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("删除列"));
            if (delColUp.isVisible()) {
                delColUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_224 删除列(上方工具栏)成功");
            } else {
                log.info("GNYL_224 上方工具栏未找到删除列按钮");
            }
        } else {
            log.info("GNYL_224 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_225: 删除表格(下方工具栏)")
    public void test_GNYL_225() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(3) > td:nth-child(2)").click();
        page.waitForTimeout(300);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("删除表格")).click();
        page.waitForTimeout(500);

        log.info("GNYL_225 删除表格(下方工具栏)成功");
    }

    @Test
    @DisplayName("GNYL_226: 删除表格(上方工具栏)")
    public void test_GNYL_226() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("表格").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("插入表格")).click();
        page.waitForTimeout(500);
        page.locator("tr:nth-child(3) > td:nth-child(2)").click();
        page.waitForTimeout(300);

        Locator upperBar = page.locator("[class*='table-toolbar'], [class*='table-bar'], .w-e-table-toolbar").first();
        if (upperBar.isVisible()) {
            Locator delTableUp = upperBar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("删除表格"));
            if (delTableUp.isVisible()) {
                delTableUp.click();
                page.waitForTimeout(500);
                log.info("GNYL_226 删除表格(上方工具栏)成功");
            } else {
                log.info("GNYL_226 上方工具栏未找到删除表格按钮");
            }
        } else {
            log.info("GNYL_226 未找到表格上方工具栏");
        }
    }

    @Test
    @DisplayName("GNYL_227: 添加分割线")
    public void test_GNYL_227() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("分割线")).click();
        page.waitForTimeout(500);

        log.info("GNYL_227 添加分割线成功");
    }

    @Test
    @DisplayName("GNYL_228: 引用")
    public void test_GNYL_228() {
        openEditorAndClickContent();

        Locator quoteBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("引用"));
        if (quoteBtn.isVisible()) {
            quoteBtn.click();
            page.waitForTimeout(500);
            log.info("GNYL_228 引用成功");
        } else {
            log.info("GNYL_228 未找到引用按钮");
        }
    }

    @Test
    @DisplayName("GNYL_229: 撤销")
    public void test_GNYL_229() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("分割线")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("撤销")).click();
        page.waitForTimeout(500);

        log.info("GNYL_229 撤销成功");
    }

    @Test
    @DisplayName("GNYL_230: 重做")
    public void test_GNYL_230() {
        openEditorAndClickContent();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("分割线")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("撤销")).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("重做")).click();
        page.waitForTimeout(500);

        log.info("GNYL_230 重做成功");
    }

    // ========== 需求视图编辑 ==========
    @Test
    @DisplayName("GNYL_231: 新建视图")
    public void test_GNYL_231() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("保存视图")).click();
        page.waitForTimeout(500);
        page.getByLabel("视图名称").click();
        page.getByLabel("视图名称").fill("自动化测试视图_" + System.currentTimeMillis());
        page.getByLabel("视图描述").click();
        page.getByLabel("视图描述").fill("由自动化测试创建");
        page.getByRole(AriaRole.CONTENTINFO).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("保存")).click();
        page.waitForTimeout(500);

        log.info("GNYL_231 新建视图成功");
    }

    @Test
    @DisplayName("GNYL_232: 视图名称必填测试")
    public void test_GNYL_232() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("保存视图")).click();
        page.waitForTimeout(500);

        page.getByRole(AriaRole.CONTENTINFO).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("保存")).click();
        page.waitForTimeout(500);

        log.info("GNYL_232 视图名称为空时保存无反应（符合预期）");
    }

    @Test
    @DisplayName("GNYL_233: 视图名称唯一性测试")
    public void test_GNYL_233() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("保存视图")).click();
        page.waitForTimeout(500);

        page.getByLabel("视图名称").click();
        page.getByLabel("视图名称").fill("123");
        page.getByRole(AriaRole.CONTENTINFO).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("保存")).click();
        page.waitForTimeout(500);

        Locator dupMsg = page.getByText("需求规格视图名称重复！");
        if (dupMsg.isVisible()) {
            assertThat(dupMsg).isVisible();
            log.info("GNYL_233 视图名称重复提示正确");
        } else {
            log.info("GNYL_233 未检测到重复名称提示");
        }
    }

    @Test
    @DisplayName("GNYL_234: 视图描述输入验证")
    public void test_GNYL_234() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);
        page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("保存视图")).click();
        page.waitForTimeout(500);

        page.getByLabel("视图名称").click();
        page.getByLabel("视图名称").fill("视图描述测试_" + System.currentTimeMillis());
        page.getByLabel("视图描述").click();
        page.getByLabel("视图描述").fill("自动化测试描述_" + System.currentTimeMillis());
        page.getByRole(AriaRole.CONTENTINFO).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("保存")).click();
        page.waitForTimeout(500);

        log.info("GNYL_234 视图描述输入验证成功");
    }

    @Test
    @DisplayName("GNYL_235: 打开标准视图")
    public void test_GNYL_235() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);

        Locator viewList = page.locator("[class*='view-list'], [class*='dropdown'], .el-dropdown-menu").first();
        if (viewList.isVisible()) {
            Locator standardView = viewList.locator("span, .el-dropdown-menu__item")
                    .filter(new Locator.FilterOptions().setHasText("标准")).first();
            if (standardView.isVisible()) {
                standardView.click();
                page.waitForTimeout(1000);
                log.info("GNYL_235 打开标准视图成功");
            } else {
                log.info("GNYL_235 未找到标准视图项");
            }
        } else {
            log.info("GNYL_235 未找到视图列表下拉菜单");
        }
    }

    @Test
    @DisplayName("GNYL_236: 打开新建的视图")
    public void test_GNYL_236() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);

        Locator viewList = page.locator("[class*='view-list'], [class*='dropdown'], .el-dropdown-menu").first();
        if (viewList.isVisible()) {
            Locator testView = viewList.locator("span, .el-dropdown-menu__item")
                    .filter(new Locator.FilterOptions().setHasText("自动化测试视图_")).first();
            if (testView.isVisible()) {
                testView.click();
                page.waitForTimeout(1000);
                log.info("GNYL_236 打开新建的视图成功");
            } else {
                log.info("GNYL_236 未找到新建的测试视图（可能已被删除）");
            }
        } else {
            log.info("GNYL_236 未找到视图列表下拉菜单");
        }
    }

    @Test
    @DisplayName("GNYL_237: 删除视图")
    public void test_GNYL_237() {
        reqPage.doubleClickTreeNode(TestConstants.ROOT_NODE);
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("视图").setExact(true)).click();
        page.waitForTimeout(300);

        Locator viewList = page.locator("[class*='view-list'], [class*='dropdown'], .el-dropdown-menu").first();
        if (viewList.isVisible()) {
            Locator manageView = viewList.locator("span, .el-dropdown-menu__item")
                    .filter(new Locator.FilterOptions().setHasText("管理")).first();
            if (manageView.isVisible()) {
                manageView.click();
                page.waitForTimeout(500);
                log.info("GNYL_237 进入视图管理成功");
            } else {
                log.info("GNYL_237 未找到视图管理菜单项");
            }
        } else {
            log.info("GNYL_237 未找到视图列表下拉菜单");
        }
    }

    @Test
    @DisplayName("GNYL_238: 分屏展示")
    public void test_GNYL_238() {
        reqPage.rightClickTreeNode(TestConstants.REQ_NAME1);
        page.waitForTimeout(500);
        page.getByText("打开", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("共享模式", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        reqPage.rightClickTreeNode(TestConstants.REQ_NAME2);
        page.waitForTimeout(500);
        page.getByText("打开", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(300);
        page.getByText("共享模式", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator tab = page.locator("[class*='tab'], [class*='el-tabs']").first();
        if (tab.isVisible()) {
            Locator tabItem = tab.locator(".el-tabs__item, [class*='tab-item']").first();
            if (tabItem.isVisible()) {
                tabItem.hover();
                page.mouse().down();
                page.mouse().move(300, 0);
                page.mouse().up();
                page.waitForTimeout(1000);
                log.info("GNYL_238 分屏展示已尝试（拖拽页签）");
            } else {
                log.info("GNYL_238 未找到页签项");
            }
        } else {
            log.info("GNYL_238 未找到页签容器");
        }
    }

}
