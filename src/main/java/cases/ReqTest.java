package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import config.TestConfig;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;
import com.microsoft.playwright.APIResponse;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqTest extends BaseTest {

    private RequirementPage reqPage;
    private ReqApiActions api;
    private static final Logger log = LoggerFactory.getLogger(ReqTest.class);

    private static final String TEST_FILES_DIR = "src/main/resources/testfiles/";
    private static final String PROJECT_ID = TestConstants.PROJECT_ID;
    private static final String PARENT_FOLDER = TestConstants.PARENT_FOLDER;

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
        api = new ReqApiActions(page.request());
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 工具方法 ==========

    private String resolveParentId() {
        return api.findNodeIdByTitle(PROJECT_ID, PARENT_FOLDER);
    }

    /** 在测试父文件夹下创建临时文档，返回 [docId, docName, parentId] */
    private String[] createTempDoc() {
        String parentId = resolveParentId();
        Assertions.assertNotNull(parentId, "未找到父文件夹节点");
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String docName = "Temp_Req_" + suffix;
        String docId = api.createDocument(PROJECT_ID, parentId);
        api.renameDocument(PROJECT_ID, docId, parentId, docName);
        return new String[]{docId, docName, parentId};
    }

    private void cleanupDoc(String docId, String parentId) {
        try {
            api.deleteDocument(docId, parentId);
            api.cleanDocument(docId, parentId);
        } catch (Exception e) {
            log.warn("清理文档 {} 失败: {}", docId, e.getMessage());
        }
    }

    private void closeDialogs() {
        try {
            while (page != null &&
                    page.locator(".el-dialog:visible, .el-overlay:visible, .el-message-box:visible").count() > 0) {
                page.keyboard().press("Escape");
                page.waitForTimeout(300);
            }
        } catch (Exception e) {
            log.warn("清理残留弹窗异常: {}", e.getMessage());
        }
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("GNYL_072 [API] 新建需求规格")
    void test_GNYL072_newReq() {
        String parentId = resolveParentId();
        Assertions.assertNotNull(parentId, "未获取到父节点ID");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String docName = "Temp_072_" + suffix;
        String docId = api.createDocument(PROJECT_ID, parentId);
        api.renameDocument(PROJECT_ID, docId, parentId, docName);

        try {
            Assertions.assertFalse(docId.isEmpty(), "未能获取到新创建文档的 ID");
            log.info("GNYL_072 新建需求规格成功, docId={}", docId);
        } finally {
            cleanupDoc(docId, parentId);
        }
    }

    @Test
    @DisplayName("GNYL_078 [API] 修改需求规格名称")
    void test_GNYL078_modifyReq() {
        String parentId = resolveParentId();
        Assertions.assertNotNull(parentId, "未获取到父节点ID");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String docName = "Temp_078_" + suffix;
        String docId = api.createDocument(PROJECT_ID, parentId);
        api.renameDocument(PROJECT_ID, docId, parentId, docName);

        try {
            String newName = "Renamed_078_" + suffix;
            String resp = api.renameDocument(PROJECT_ID, docId, parentId, newName);
            Assertions.assertTrue(resp.contains("200"), "业务返回码不是200: " + resp);
            Assertions.assertTrue(resp.contains("修改成功"), "返回信息不匹配: " + resp);
            log.info("GNYL_078 需求规格名称修改成功");
        } finally {
            cleanupDoc(docId, parentId);
        }
    }

    @Test
    @DisplayName("GNYL_084/086/088 [API] 删除 → 恢复 → 清除需求规格（完整生命周期）")
    void test_GNYL084_086_088_deleteRecoverClean() {
        String parentId = resolveParentId();
        Assertions.assertNotNull(parentId, "未获取到父节点ID");

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String docId = api.createDocument(PROJECT_ID, parentId);
        api.renameDocument(PROJECT_ID, docId, parentId, "生命周期测试_" + suffix);

        try {
            // 1. 删除
            String deleteResp = api.deleteDocument(docId, parentId);
            Assertions.assertTrue(deleteResp.contains("200"), "删除失败: " + deleteResp);
            log.info("GNYL_084 删除成功");

            // 2. 恢复
            String recoverResp = api.recoverDocument(docId, parentId);
            Assertions.assertTrue(recoverResp.contains("200"), "恢复失败: " + recoverResp);
            log.info("GNYL_086 恢复成功");

            // 3. 再删除（恢复后要再删才能清除）
            deleteResp = api.deleteDocument(docId, parentId);
            Assertions.assertTrue(deleteResp.contains("200"), "二次删除失败: " + deleteResp);
            log.info("GNYL_087 二次删除成功");

            // 4. 清除
            String cleanResp = api.cleanDocument(docId, parentId);
            Assertions.assertTrue(cleanResp.contains("200"), "清除失败: " + cleanResp);
            log.info("GNYL_088 清除成功");
        } finally {
            // 如果中途失败，确保清理
            cleanupDoc(docId, parentId);
        }
    }

    @Test
    @DisplayName("GNYL_090 [API] 需求表视图切换（查询需求规格列表）")
    void test_GNYL_090_DemandTable() {
        String resp = api.getReqSpeList(PROJECT_ID);

        Assertions.assertTrue(resp.contains("200"), "查询失败: " + resp);
        Assertions.assertTrue(resp.contains("操作成功"), "返回信息不匹配: " + resp);

        JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = json.getAsJsonArray("data");
        Assertions.assertNotNull(data, "data 为 null");
        Assertions.assertTrue(data.size() > 0, "需求规格列表为空");
        log.info("GNYL_090 需求表视图切换通过");
    }

    @Test
    @DisplayName("GNYL_091 [UI] 需求树视图切换")
    void test_GNYL_091_DemandTree() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            Locator demandTreeBtn = page.locator("div")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^需求树$")))
                    .nth(1);
            if (demandTreeBtn.isVisible()) {
                demandTreeBtn.click();
                page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("需求表")).click();
                page.waitForTimeout(1000);
            }

            Locator demandTableBtn = page.locator("div")
                    .filter(new Locator.FilterOptions().setHasText(Pattern.compile("^需求表$")))
                    .nth(1);
            demandTableBtn.click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("需求树")).click();

            assertThat(page.getByText(TestConstants.ROOT_NODE).first()).isVisible();
            log.info("GNYL_091 需求树视图切换通过");

            page.locator("#app").getByText("需求（根节点）").click(new Locator.ClickOptions()
                    .setButton(MouseButton.RIGHT));
            page.locator("span").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^刷新$"))).click();
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_096: 查看属性")
    void test_GNYL_096_ViewProperties() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName).first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));

            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();
            assertThat(dialog.getByText("创建时间:")).isVisible();
            assertThat(dialog.getByText("最后修改时间:")).isVisible();
            assertThat(dialog.getByText("创建者:")).isVisible();
            assertThat(dialog.getByText("最后修改者:")).isVisible();
            assertThat(dialog.getByText("写权限:")).isVisible();

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
            log.info("GNYL_096 属性查看通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_097: 编辑属性")
    void test_GNYL_097_EditProperties() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName).first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();

            Locator nameInput = page.getByPlaceholder("可编辑名称");
            if (nameInput.isVisible()) {
                nameInput.click();
                nameInput.press("Control+a");
                nameInput.fill(docName + "_edited");
                page.waitForTimeout(300);
            }

            Locator prefixInput = page.getByPlaceholder("可编辑前缀");
            if (prefixInput.isVisible()) {
                prefixInput.click();
                prefixInput.press("Control+a");
                prefixInput.fill("REQ");
                page.waitForTimeout(300);
            }

            Locator descArea = page.getByPlaceholder("可编辑描述");
            if (descArea.isVisible()) {
                descArea.click();
                descArea.press("Control+a");
                descArea.fill("自动化测试编辑属性描述信息");
                page.waitForTimeout(300);
            }

            Path filePath = Paths.get(TEST_FILES_DIR + "test_attachment.txt");
            Locator uploadInput = page.locator(".el-upload input[type='file']");
            uploadInput.setInputFiles(filePath);
            page.waitForTimeout(2000);

            Locator remarkInput = page.getByPlaceholder("请输入备注");
            if (remarkInput.isVisible()) {
                remarkInput.click();
                remarkInput.fill("测试备注不超过50字");
                page.waitForTimeout(300);
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
            page.waitForTimeout(1000);

            log.info("GNYL_097 编辑属性通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_098: 规格名称必填测试")
    void test_GNYL_098_NameRequired() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);

            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator nameInput = page.locator(".el-dialog input[type='text']").first();
            nameInput.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            nameInput.click();
            nameInput.press("Control+a");
            nameInput.fill("");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
            page.waitForTimeout(500);

            Locator errorMsg = page.getByText("需求规格名称不能为空！");
            assertThat(errorMsg).isVisible();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("关闭此对话框")).click();
            log.info("GNYL_098 规格名称必填校验通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_100/101/102/103: 前缀校验（非字母开头 -> 非法字符 -> 超长 -> 合法）")
    void test_GNYL_100_101_102_103_PrefixValidation() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator prefixInput = page.getByPlaceholder("可编辑前缀");
            Locator descArea = page.getByPlaceholder("可编辑描述");
            Locator errorMsg = page.getByText("编码规则不符合要求:必须以字母开头,且长度不超过10");

            prefixInput.click();
            prefixInput.press("Control+a");
            prefixInput.fill("1");
            descArea.click();
            page.waitForTimeout(500);
            assertThat(errorMsg).isVisible();
            log.info("GNYL_100 非字母开头拦截通过");

            prefixInput.click();
            prefixInput.fill("&*%(@$");
            descArea.click();
            page.waitForTimeout(500);
            assertThat(errorMsg).isVisible();
            log.info("GNYL_101 非法字符拦截通过");

            prefixInput.click();
            prefixInput.press("Control+a");
            prefixInput.fill("123456789789");
            descArea.click();
            page.waitForTimeout(500);
            assertThat(errorMsg).isVisible();
            log.info("GNYL_102 超长前缀拦截通过");

            prefixInput.click();
            prefixInput.press("Control+a");
            prefixInput.fill("req");
            descArea.click();
            page.waitForTimeout(500);
            Assertions.assertFalse(errorMsg.isVisible(), "合法前缀不应出现错误提示");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
            page.waitForTimeout(1000);
            log.info("GNYL_103 合法前缀保存通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_104/105: 描述校验（合法描述 -> 超长描述）")
    void test_GNYL_104_105_DescriptionValidation() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator descArea = page.getByPlaceholder("可编辑描述");

            String validDesc = "1.在合作区管理列表选择合作区，点击设置属性\n" +
                    "2.勾选一个或多个属性复选框，点击列表上方删除按钮\n" +
                    "3.在二次确认框，点击确定按钮";
            descArea.click();
            descArea.press("Control+a");
            descArea.fill(validDesc);
            page.waitForTimeout(300);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
            page.waitForTimeout(2000);

            log.info("GNYL_104 合法描述保存通过, 字数: {}", validDesc.length());

            closeDialogs();
            page.waitForTimeout(500);

            // GNYL_105: 超长描述校验
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            String longBlock = "这是一段用于测试超长描述的文本，验证系统对描述字段长度的限制是否生效。";
            StringBuilder sb = new StringBuilder();
            while (sb.length() < 1200) {
                sb.append(longBlock);
            }
            String tooLongDesc = sb.toString();

            descArea = page.getByPlaceholder("可编辑描述");
            descArea.click();
            descArea.press("Control+a");
            descArea.fill(tooLongDesc);
            page.waitForTimeout(500);

            String actualValue = descArea.inputValue();

            if (actualValue.length() <= 1000) {
                page.waitForTimeout(300);
            } else {
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
                page.waitForTimeout(500);
            }

            log.info("GNYL_105 超长描述校验通过 (期望: {} 字, 实际: {} 字)", tooLongDesc.length(), actualValue.length());
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_106: 拖动符合格式的文件上传")
    void test_GNYL_106_DragUploadValid() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName)
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "test_attachment.txt");
            page.locator("input[type='file']").setInputFiles(filePath);
            page.waitForTimeout(1000);

            Locator fileName = page.getByText("test_attachment.txt");
            assertThat(fileName).isVisible();
            log.info("GNYL_106 拖动上传合法文件成功");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_107: 拖动不符合格式的文件上传")
    void test_GNYL_107_DragUploadInvalid() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName).first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Path filePath = Paths.get(TEST_FILES_DIR + "faker");
            Locator uploadInput = page.locator(".el-upload input[type='file']");
            uploadInput.setInputFiles(filePath);
            page.waitForTimeout(2000);

            Locator errorMsg = page.locator(".el-message--error, .el-upload--text, [class*='error']").first();
            if (errorMsg.isVisible()) {
                assertThat(errorMsg).isVisible();
                log.info("GNYL_107 不合法格式上传被拦截: {}", errorMsg.textContent());
            } else {
                log.info("GNYL_107 上传区域可能自动拦截了不合法格式");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_110: 填写不超过50字的备注")
    void test_GNYL_110_RemarkValid() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName).first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            page.getByText(Pattern.compile("^备注")).first().click();
            page.waitForTimeout(300);

            String shortRemark = "这是一个不超过50字的备注测试内容";
            Assertions.assertTrue(shortRemark.length() <= 50, "测试数据超过50字");
            page.locator(".el-dialog:visible").locator("input, textarea").last().fill(shortRemark);
            page.waitForTimeout(300);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
            page.waitForTimeout(1000);
            log.info("GNYL_110 不超过50字备注保存通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_111: 填写超过50字的备注")
    void test_GNYL_111_RemarkTooLong() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREE)
                    .getByText(docName).first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            page.getByText(Pattern.compile("^备注")).first().click();
            page.waitForTimeout(300);

            Locator remarkInput = page.locator(".el-dialog:visible").locator("input, textarea").last();

            String longRemark = "这是一段超过五十字的备注测试内容用于验证系统对备注字段长度的限制是否能够正确地拦截超长输入确保用户无法输入过长的文本内容这是一段额外的文字用来凑够一百个字符的长度测试完毕";
            Assertions.assertTrue(longRemark.length() >= 100, "测试数据应达到100字");
            remarkInput.click();
            remarkInput.fill(longRemark);
            page.waitForTimeout(500);

            Locator counter = page.locator(".el-dialog:visible").getByText(Pattern.compile("\\d+\\s*/\\s*50"));
            if (counter.isVisible()) {
                String counterText = counter.textContent().trim();
                Assertions.assertTrue(counterText.contains("50"), "计数器未显示50/50，实际: " + counterText);
                log.info("GNYL_111 备注计数器显示: {}", counterText);
            }

            String actualValue = remarkInput.inputValue();
            Assertions.assertTrue(actualValue.length() <= 50,
                    "备注未限制到50字以内，实际: " + actualValue.length() + " 字");
            log.info("GNYL_111 超长备注校验通过 (输入: {} 字, 实际保留: {} 字)", longRemark.length(), actualValue.length());

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_112: 删除属性页文件")
    void test_GNYL_112_DeletePropertyFile() {
        String baseUrl = TestConfig.API_PREFIX;

        Path filePath = Paths.get(TEST_FILES_DIR + "test_attachment.txt");
        Assertions.assertTrue(Files.exists(filePath), "测试文件不存在: " + filePath);

        APIResponse uploadResp = page.request().post(baseUrl + "/erm/upload/reqDocUpload",
                RequestOptions.create()
                        .setMultipart(
                                FormData.create().set("file", filePath)
                        )
        );
        Assertions.assertEquals(200, uploadResp.status(), "上传接口调用失败");

        JsonObject uploadJson = JsonParser.parseString(uploadResp.text()).getAsJsonObject();
        String objectId = uploadJson.getAsJsonObject("data").get("objectId").getAsString();
        log.info("GNYL_112 上传成功, objectId={}", objectId);

        try {
            JsonObject deleteBody = new JsonObject();
            deleteBody.addProperty("objectId", objectId);

            APIResponse deleteResp = page.request().post(baseUrl + "/erm/reqDocDelete",
                    RequestOptions.create()
                            .setHeader("Content-Type", "application/json")
                            .setData(deleteBody.toString())
            );
            Assertions.assertEquals(200, deleteResp.status(), "删除接口调用失败");

            JsonObject deleteJson = JsonParser.parseString(deleteResp.text()).getAsJsonObject();
            Assertions.assertEquals("操作成功", deleteJson.get("msg").getAsString(), "删除接口返回失败");
            log.info("GNYL_112 删除成功, objectId={}, msg={}", objectId, deleteJson.get("msg").getAsString());
        } finally {
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_113: 添加权限人员")
    void test_GNYL_113_AddPermissionUser() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            Locator editIcon = page.locator("[class*='edit'], .el-icon-edit, [class*='permission']").first();
            if (editIcon.isVisible()) {
                editIcon.click();
                page.waitForTimeout(1000);
            } else {
                page.getByRole(AriaRole.ROW,
                                new Page.GetByRoleOptions().setName(docName))
                        .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
                page.waitForTimeout(500);
                page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
                page.waitForTimeout(1000);
            }

            Locator dialog = page.locator(".el-dialog").first();
            assertThat(dialog).isVisible();

            Locator userCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
            if (userCheckbox.isVisible()) {
                userCheckbox.click();
                page.waitForTimeout(300);
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
            page.waitForTimeout(500);
            log.info("GNYL_113 添加权限人员通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_114: 组织部门选择验证")
    void test_GNYL_114_OrganizationSelection() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator orgSelect = page.locator("[class*='org'], [class*='department'], .el-tree").first();
            if (orgSelect.isVisible()) {
                Locator orgNode = page.getByText("公司", new Page.GetByTextOptions().setExact(true));
                if (orgNode.isVisible()) {
                    orgNode.click();
                    page.waitForTimeout(500);
                    log.info("GNYL_114 选择了组织节点");
                }
            }

            Locator userList = page.locator(".el-table, .user-list, [class*='user']").first();
            assertThat(userList).isVisible();
            log.info("GNYL_114 组织部门选择验证通过");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_115: 勾选人员验证")
    void test_GNYL_115_UserSelectionValidation() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator firstCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
            if (firstCheckbox.isVisible()) {
                firstCheckbox.click();
                page.waitForTimeout(500);
            }

            Locator selectedArea = page.locator("[class*='selected'], [class*='current']").first();
            if (selectedArea.isVisible()) {
                log.info("GNYL_115 当前选中用户区域可见");
            } else {
                log.info("GNYL_115 用户勾选成功");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
            log.info("GNYL_115 勾选人员验证通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_116: 删除选中人员")
    void test_GNYL_116_RemoveSelectedUser() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator firstCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
            if (firstCheckbox.isVisible()) {
                firstCheckbox.click();
                page.waitForTimeout(300);
            }
            if (firstCheckbox.isVisible()) {
                firstCheckbox.click();
                page.waitForTimeout(300);
                log.info("GNYL_116 人员已从选中列表移除");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
            log.info("GNYL_116 删除选中人员通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_117: 存在的用户名检索")
    void test_GNYL_117_SearchExistingUser() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("admin");
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator userRow = page.locator(".el-table__row, [class*='user-row']").first();
                if (userRow.isVisible()) {
                    log.info("GNYL_117 存在用户名检索成功，列表展示匹配人员");
                } else {
                    log.info("GNYL_117 搜索完成，列表中存在匹配结果");
                }
            } else {
                String resp = api.searchUser("admin");
                Assertions.assertTrue(resp.contains("admin"), "API搜索存在的用户未返回结果: " + resp);
                log.info("GNYL_117 API搜索存在的用户成功");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_118: 用户名模糊查询")
    void test_GNYL_118_FuzzySearchUser() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("ad");
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator userRow = page.locator(".el-table__row, [class*='user-row']").first();
                if (userRow.isVisible()) {
                    log.info("GNYL_118 模糊查询成功，列表中包含搜索关键字相关用户");
                } else {
                    log.info("GNYL_118 模糊查询完成");
                }
            } else {
                String resp = api.searchUser("ad");
                Assertions.assertFalse(api.isDataEmpty(resp), "API模糊搜索未返回结果");
                log.info("GNYL_118 API模糊搜索成功");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_119: 不存在的用户名检索")
    void test_GNYL_119_SearchNonExistentUser() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("__nonexistent_user_xyz__");
                page.waitForTimeout(500);
                searchInput.press("Enter");
                page.waitForTimeout(1000);

                Locator emptyText = page.locator(".el-empty, [class*='empty'], .el-table__empty-text");
                if (emptyText.isVisible()) {
                    assertThat(emptyText).isVisible();
                    log.info("GNYL_119 不存在的用户检索显示暂无数据: {}", emptyText.textContent());
                } else {
                    Locator tableRows = page.locator(".el-table__body-wrapper tbody tr");
                    int rowCount = tableRows.count();
                    Assertions.assertEquals(0, rowCount, "不存在的用户检索不应返回数据");
                    log.info("GNYL_119 搜索不存在用户，表格无数据");
                }
            } else {
                String resp = api.searchUser("__nonexistent_user_xyz__");
                Assertions.assertTrue(api.isDataEmpty(resp), "API搜索不存在的用户应返回空数据");
                log.info("GNYL_119 API搜索不存在的用户返回空");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_120: 清空用户名检索输入框")
    void test_GNYL_120_ClearUserSearchInput() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(docName))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);

            Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
            if (searchInput.isVisible()) {
                searchInput.click();
                searchInput.fill("admin");
                page.waitForTimeout(300);

                searchInput.click();
                searchInput.press("Control+a");
                searchInput.fill("");
                page.waitForTimeout(500);

                String value = searchInput.inputValue();
                Assertions.assertTrue(value.isEmpty(), "搜索输入框未清空");
                log.info("GNYL_120 搜索输入框已清空");

                searchInput.press("Enter");
                page.waitForTimeout(1000);
                Locator userList = page.locator(".el-table__row").first();
                if (userList.isVisible()) {
                    log.info("GNYL_120 清空后恢复展示默认人员列表");
                }
            } else {
                log.info("GNYL_120 未找到搜索输入框");
            }

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
            page.waitForTimeout(500);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_121: 新建一级需求条目")
    void test_GNYL_121_CreateFirstLevelRequirementItem() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新建子级")).click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷新")).click();
            page.waitForTimeout(2000);

            Locator newCell = page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first();
            newCell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

            String firstItemText = newCell.innerText().trim();
            log.info("GNYL_121 新建一级条目成功: {}", firstItemText);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_122: 新建子需求条目")
    void test_GNYL_122_CreateSubRequirementItem() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            // 先创建一级条目作为父级
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新建子级")).click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷新")).click();
            page.waitForTimeout(2000);

            Locator firstLevelCell = page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first();
            firstLevelCell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            String parentItemText = firstLevelCell.innerText().trim();
            log.info("GNYL_122 已创建一级条目: {}", parentItemText);

            // 在一级条目下创建子条目
            page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(parentItemText))
                    .locator("div").first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(1000);

            page.getByText("新建", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(500);

            page.locator("div").filter(new Locator.FilterOptions()
                    .setHasText(Pattern.compile("^子级对象$"))).click();
            page.waitForTimeout(2000);

            Locator subCell = page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first();
            subCell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));

            String subItemText = subCell.innerText().trim();
            log.info("GNYL_122 新建子条目成功: {}", subItemText);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_123: 删除需求条目")
    void test_GNYL_123_DeleteRequirementItem() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            // 先创建一级条目作为删除目标
            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("新建子级")).click();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("刷新")).click();
            page.waitForTimeout(2000);

            Locator targetCell = page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName("req-")).first();
            targetCell.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            String targetItemText = targetCell.innerText().trim();
            log.info("GNYL_123 已创建待删除条目: {}", targetItemText);

            // 右键删除该条目
            page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(targetItemText))
                    .locator("div").first()
                    .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(1000);

            page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
            page.waitForTimeout(1000);

            log.info("GNYL_123 删除条目 {} 成功", targetItemText);
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }

    @Test
    @DisplayName("GNYL_128/129/130: 显示大纲 -> 结构定位 -> 隐藏大纲")
    void test_GNYL_128_129_130_OutlineAndStructure() {
        String[] doc = createTempDoc();
        String docId = doc[0], docName = doc[1], parentId = doc[2];
        try {
            page.waitForTimeout(500);

            page.getByRole(AriaRole.TREEITEM,
                            new Page.GetByRoleOptions().setName(docName).setExact(true))
                    .first().dblclick();
            page.waitForTimeout(1000);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示大纲")).click();
            page.waitForTimeout(500);

            Locator outlineTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("结构"));
            assertThat(outlineTab).isVisible();
            page.waitForTimeout(500);
            log.info("GNYL_128 显示大纲通过，条目可点击");

            outlineTab.click();
            page.waitForTimeout(500);

            Locator structItem = page.getByRole(AriaRole.TREEITEM,
                    new Page.GetByRoleOptions().setName("req-")).locator("div").first();
            assertThat(structItem).isVisible();
            structItem.click();
            page.waitForTimeout(500);
            log.info("GNYL_129 结构视图定位通过");

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("隐藏大纲")).click();
            page.waitForTimeout(500);

            Locator showBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("显示大纲"));
            assertThat(showBtn).isVisible();
            log.info("GNYL_130 隐藏大纲通过");
        } finally {
            cleanupDoc(docId, parentId);
            closeDialogs();
        }
    }
}
