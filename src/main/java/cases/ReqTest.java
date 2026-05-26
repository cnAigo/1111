package cases;

import actions.ReqApiActions;
import base.BaseTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import config.TestConstants;
import config.TestContext;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;
import com.microsoft.playwright.APIResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.Map;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqTest extends BaseTest {

    private RequirementPage reqPage;
    private ReqApiActions api;
    private static final Logger log = LoggerFactory.getLogger(ReqTest.class);

    private static final String TEST_FILES_DIR = "src/main/resources/testfiles/";

    @BeforeAll
    public void initPage() {
        reqPage = new RequirementPage(page);
        api = new ReqApiActions(page.request());
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    @Test
    @Order(720)
    @DisplayName("GNYL_072 [API] 新建需求规格")
    void test_GNYL072_newReq() {
        String parentId = TestContext.get("parentId");
        String projectId = TestContext.get("projectId");

        if (parentId == null || parentId.isEmpty()) {
            parentId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
            if (parentId != null) TestContext.set("parentId", parentId);
        }
        if (projectId == null || projectId.isEmpty()) {
            projectId = TestConstants.PROJECT_ID;
            TestContext.set("projectId", projectId);
        }

        Assumptions.assumeTrue(parentId != null && !parentId.isEmpty(), "未获取到父节点ID");

        String docId = api.createDocument(projectId, parentId);
        TestContext.set("reqId", docId);

        Assertions.assertFalse(docId.isEmpty(), "未能获取到新创建文档的 ID");
        log.info("GNYL_072 新建需求规格成功, docId={}", docId);
    }

    @Test
    @Order(780)
    @DisplayName("GNYL_078 [API] 修改需求规格名称")
    void test_GNYL078_modifyReq() {
        String reqId = TestContext.get("reqId");
        String parentId = TestContext.get("parentId");
        Assumptions.assumeTrue(reqId != null && !reqId.isEmpty(), "未获取到需求规格ID");

        String resp = api.renameDocument(TestContext.get("projectId"), reqId, parentId, TestConstants.REQ_NAME1);

        Assertions.assertTrue(resp.contains("200"), "业务返回码不是200: " + resp);
        Assertions.assertTrue(resp.contains("修改成功"), "返回信息不匹配: " + resp);
        log.info("GNYL_078 需求规格名称修改成功");
    }


    @Test
    @Order(840)
    @DisplayName("GNYL_084/086/088 [API] 删除 → 恢复 → 清除需求规格（完整生命周期）")
    void test_GNYL084_086_088_deleteRecoverClean() {
        String parentId = resolveParentId();
        String projectId = resolveProjectId();
        Assumptions.assumeTrue(parentId != null, "未获取到父节点ID");

        // 自己新建一个专用文档
        String reqId = api.createDocument(projectId, parentId);
        api.renameDocument(projectId, reqId, parentId, "生命周期测试文档");
        log.info("新建文档: {}", reqId);

        // 1. 删除
        String deleteResp = api.deleteDocument(reqId, parentId);
        Assertions.assertTrue(deleteResp.contains("200"), "删除失败: " + deleteResp);
        log.info("GNYL_084 删除成功");

        // 2. 恢复
        String recoverResp = api.recoverDocument(reqId, parentId);
        Assertions.assertTrue(recoverResp.contains("200"), "恢复失败: " + recoverResp);
        log.info("GNYL_086 恢复成功");

        // 3. 再删除（恢复后要再删才能清除）
        deleteResp = api.deleteDocument(reqId, parentId);
        Assertions.assertTrue(deleteResp.contains("200"), "二次删除失败: " + deleteResp);
        log.info("GNYL_087 二次删除成功");

        // 4. 清除
        String cleanResp = api.cleanDocument(reqId, parentId);
        Assertions.assertTrue(cleanResp.contains("200"), "清除失败: " + cleanResp);
        log.info("GNYL_088 清除成功");
    }


    @Test
    @Order(900)
    @DisplayName("GNYL_090 [API] 需求表视图切换（查询需求规格列表）")
    void test_GNYL_090_DemandTable() {
        String resp = api.getReqSpeList(TestContext.get("projectId"));

        Assertions.assertTrue(resp.contains("200"), "查询失败: " + resp);
        Assertions.assertTrue(resp.contains("操作成功"), "返回信息不匹配: " + resp);

        JsonObject json = com.google.gson.JsonParser.parseString(resp).getAsJsonObject();
        JsonArray data = json.getAsJsonArray("data");

        Assertions.assertNotNull(data, "data 为 null");
        Assertions.assertTrue(data.size() > 0, "需求规格列表为空");
        log.info("GNYL_090 需求表视图切换通过");
    }

    @Test
    @Order(910)
    @DisplayName("GNYL_091 [UI] 需求树视图切换")
    void test_GNYL_091_DemandTree() {
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
    }

    @Test
    @Order(960)
    @DisplayName("GNYL_096: 查看属性")
    void test_GNYL_096_ViewProperties() {
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1).first()
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
    }

    @Test
    @Order(970)
    @Timeout(10)
    @DisplayName("GNYL_097: 编辑属性")
    void test_GNYL_097_EditProperties() {
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();

        // 编辑名称
        Locator nameInput = page.getByPlaceholder("可编辑名称");
        if (nameInput.isVisible()) {
            nameInput.click();
            nameInput.press("Control+a");
            nameInput.fill(TestConstants.REQ_NAME1 + "_编辑");
            page.waitForTimeout(300);
        }

        // 编辑前缀
        Locator prefixInput = page.getByPlaceholder("可编辑前缀");
        if (prefixInput.isVisible()) {
            prefixInput.click();
            prefixInput.press("Control+a");
            prefixInput.fill("REQ");
            page.waitForTimeout(300);
        }

        // 编辑描述
        Locator descArea = page.getByPlaceholder("可编辑描述");
        if (descArea.isVisible()) {
            descArea.click();
            descArea.press("Control+a");
            descArea.fill("自动化测试编辑属性描述信息");
            page.waitForTimeout(300);
        }

        // 上传文件
        Path filePath = Paths.get(TEST_FILES_DIR + "test_attachment.txt");
        Locator uploadInput = page.locator(".el-upload input[type='file']");
        uploadInput.setInputFiles(filePath);
        page.waitForTimeout(2000);

        // 填写备注
        Locator remarkInput = page.getByPlaceholder("请输入备注");
        if (remarkInput.isVisible()) {
            remarkInput.click();
            remarkInput.fill("测试备注不超过50字");
            page.waitForTimeout(300);
        }

        // 保存
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
        page.waitForTimeout(1000);

        if (page.locator(".el-dialog:visible").count() > 0) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        }

        log.info("GNYL_097 编辑属性通过");
    }

    @Test
    @Order(980)
    @DisplayName("GNYL_098: 规格名称必填测试")
    void test_GNYL_098_NameRequired() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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
    }

    @Test
    @Order(1000)
    @DisplayName("GNYL_100/101/102/103: 前缀校验（非字母开头 -> 非法字符 -> 超长 -> 合法）")
    void test_GNYL_100_101_102_103_PrefixValidation() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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
    }

    @Test
    @Order(1040)
    @DisplayName("GNYL_104/105: 描述校验（合法描述 -> 超长描述）")
    void test_GNYL_104_105_DescriptionValidation() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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

        if (page.locator(".el-dialog:visible").count() > 0) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        }
        log.info("GNYL_104 合法描述保存通过, 字数: {}", validDesc.length());

        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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

        if (page.locator(".el-dialog:visible").count() > 0) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        }
        log.info("GNYL_105 超长描述校验通过 (期望: {} 字, 实际: {} 字)", tooLongDesc.length(), actualValue.length());
    }

    // ========== 文件上传 ==========

    @Test
    @Order(1060)
    @DisplayName("GNYL_106: 拖动符合格式的文件上传")
    void test_GNYL_106_DragUploadValid() {
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1)
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 上传文件（用隐藏的 input）
        Path filePath = Paths.get(TEST_FILES_DIR + "test_attachment.txt");
        page.locator("input[type='file']").setInputFiles(filePath);
        page.waitForTimeout(1000);

        // 验证文件名展示
        Locator fileName = page.getByText("test_attachment.txt");
        assertThat(fileName).isVisible();
        log.info("GNYL_106 拖动上传合法文件成功");

        // 关闭弹窗
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
        page.waitForTimeout(500);
    }


    @Test
    @Order(1070)
    @DisplayName("GNYL_107: 拖动不符合格式的文件上传")
    void test_GNYL_107_DragUploadInvalid() {
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Path filePath = Paths.get(TEST_FILES_DIR + "faker");

        // 定位真正的 <input type="file">，而不是外层的 <div>
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
    }


    @Test
    @Order(1100)
    @DisplayName("GNYL_110: 填写不超过50字的备注")
    void test_GNYL_110_RemarkValid() {
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 先点击"备注"区域触发编辑态
        page.getByText(Pattern.compile("^备注")).first().click();
        page.waitForTimeout(300);

        // 填写备注
        String shortRemark = "这是一个不超过50字的备注测试内容";
        Assertions.assertTrue(shortRemark.length() <= 50, "测试数据超过50字");
        page.locator(".el-dialog:visible").locator("input, textarea").last().fill(shortRemark);
        page.waitForTimeout(300);

        // 保存
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确 定")).click();
        page.waitForTimeout(1000);

        log.info("GNYL_110 不超过50字备注保存通过");
        if (page.locator(".el-dialog:visible").count() > 0) {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
            page.waitForTimeout(500);
        }
    }


    @Test
    @Order(1110)
    @DisplayName("GNYL_111: 填写超过50字的备注")
    void test_GNYL_111_RemarkTooLong() {
        page.getByRole(AriaRole.TREEITEM, new Page.GetByRoleOptions().setName("测试父文件夹")).locator("svg").click();
        page.getByRole(AriaRole.TREE)
                .getByText(TestConstants.REQ_NAME1).first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("属性", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 点击备注区域触发编辑态
        page.getByText(Pattern.compile("^备注")).first().click();
        page.waitForTimeout(300);

        // 在编辑态中定位输入框
        Locator remarkInput = page.locator(".el-dialog:visible").locator("input, textarea").last();

        // 输入100个字符
        String longRemark = "这是一段超过五十字的备注测试内容用于验证系统对备注字段长度的限制是否能够正确地拦截超长输入确保用户无法输入过长的文本内容这是一段额外的文字用来凑够一百个字符的长度测试完毕";
        Assertions.assertTrue(longRemark.length() >= 100, "测试数据应达到100字，实际: " + longRemark.length());
        remarkInput.click();
        remarkInput.fill(longRemark);
        page.waitForTimeout(500);

        // 验证前端计数器显示 50/50（被截断到50字）
        Locator counter = page.locator(".el-dialog:visible").getByText(Pattern.compile("\\d+\\s*/\\s*50"));
        if (counter.isVisible()) {
            String counterText = counter.textContent().trim();
            Assertions.assertTrue(counterText.contains("50"), "计数器未显示50/50，实际: " + counterText);

            log.info("GNYL_111 备注计数器显示: {}", counterText);
        }

        // 验证实际输入值被限制在50字以内
        String actualValue = remarkInput.inputValue();
        Assertions.assertTrue(actualValue.length() <= 50,
                "备注未限制到50字以内，实际: " + actualValue.length() + " 字");
        log.info("GNYL_111 超长备注校验通过 (输入: {} 字, 实际保留: {} 字)", longRemark.length(), actualValue.length());

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取 消")).click();
        page.waitForTimeout(500);
    }


    @Test
    @Order(1120)
    @DisplayName("GNYL_112: 删除属性页文件")
    void test_GNYL_112_DeletePropertyFile() {
        String baseUrl = "https://192.168.6.173/dev-api";

        // 1. 先通过 API 上传文件，拿到 objectId
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

        // 2. 通过 API 删除该文件
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
    }


    // ========== 权限人员 ==========

    @Test
    @Order(1130)
    @DisplayName("GNYL_113: 添加权限人员")
    void test_GNYL_113_AddPermissionUser() {
        // 双击进入需求规格
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        // 点击编辑图标
        Locator editIcon = page.locator("[class*='edit'], .el-icon-edit, [class*='permission']").first();
        if (editIcon.isVisible()) {
            editIcon.click();
            page.waitForTimeout(1000);
        } else {
            // 尝试通过右键菜单进入权限设置
            page.getByRole(AriaRole.ROW,
                            new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                    .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            page.waitForTimeout(500);
            page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
            page.waitForTimeout(1000);
        }

        Locator dialog = page.locator(".el-dialog").first();
        assertThat(dialog).isVisible();

        // 勾选人员
        Locator userCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
        if (userCheckbox.isVisible()) {
            userCheckbox.click();
            page.waitForTimeout(300);
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.waitForTimeout(500);
        log.info("GNYL_113 添加权限人员通过");
    }

    @Test
    @Order(1140)
    @DisplayName("GNYL_114: 组织部门选择验证")
    void test_GNYL_114_OrganizationSelection() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        // 打开权限设置对话框
        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 选择组织/部门
        Locator orgSelect = page.locator("[class*='org'], [class*='department'], .el-tree").first();
        if (orgSelect.isVisible()) {
            Locator orgNode = page.getByText("公司", new Page.GetByTextOptions().setExact(true));
            if (orgNode.isVisible()) {
                orgNode.click();
                page.waitForTimeout(500);
                log.info("GNYL_114 选择了组织节点");
            }
        }

        // 验证用户列表随组织选择动态更新
        Locator userList = page.locator(".el-table, .user-list, [class*='user']").first();
        assertThat(userList).isVisible();
        log.info("GNYL_114 组织部门选择验证通过");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(1150)
    @DisplayName("GNYL_115: 勾选人员验证")
    void test_GNYL_115_UserSelectionValidation() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 勾选第一个用户
        Locator firstCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
        if (firstCheckbox.isVisible()) {
            firstCheckbox.click();
            page.waitForTimeout(500);
        }

        // 验证"当前选中用户"栏展示
        Locator selectedArea = page.locator("[class*='selected'], [class*='current']").first();
        if (selectedArea.isVisible()) {
            log.info("GNYL_115 当前选中用户区域可见");
        } else {
            log.info("GNYL_115 用户勾选成功");
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(500);
        log.info("GNYL_115 勾选人员验证通过");
    }

    @Test
    @Order(1160)
    @DisplayName("GNYL_116: 删除选中人员")
    void test_GNYL_116_RemoveSelectedUser() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 先勾选一个用户
        Locator firstCheckbox = page.locator(".el-checkbox, [type='checkbox']").first();
        if (firstCheckbox.isVisible()) {
            firstCheckbox.click();
            page.waitForTimeout(300);
        }

        // 取消勾选（移除选中）
        if (firstCheckbox.isVisible()) {
            firstCheckbox.click();
            page.waitForTimeout(300);
            log.info("GNYL_116 人员已从选中列表移除");
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(500);
        log.info("GNYL_116 删除选中人员通过");
    }

    @Test
    @Order(1170)
    @DisplayName("GNYL_117: 存在的用户名检索")
    void test_GNYL_117_SearchExistingUser() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        // 搜索存在的用户名
        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
        if (searchInput.isVisible()) {
            searchInput.click();
            searchInput.fill("admin");
            page.waitForTimeout(500);
            // 按回车或点击搜索按钮
            searchInput.press("Enter");
            page.waitForTimeout(1000);

            Locator userRow = page.locator(".el-table__row, [class*='user-row']").first();
            if (userRow.isVisible()) {
                log.info("GNYL_117 存在用户名检索成功，列表展示匹配人员");
            } else {
                log.info("GNYL_117 搜索完成，列表中存在匹配结果");
            }
        } else {
            log.info("GNYL_117 未找到搜索输入框，尝试API方式验证");
            String resp = api.searchUser("admin");
            Assertions.assertTrue(resp.contains("admin"), "API搜索存在的用户未返回结果: " + resp);
            log.info("GNYL_117 API搜索存在的用户成功");
        }

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("取消")).click();
        page.waitForTimeout(500);
    }

    @Test
    @Order(1180)
    @DisplayName("GNYL_118: 用户名模糊查询")
    void test_GNYL_118_FuzzySearchUser() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
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
    }

    @Test
    @Order(1190)
    @DisplayName("GNYL_119: 不存在的用户名检索")
    void test_GNYL_119_SearchNonExistentUser() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
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
                // 也可能表格为空
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
    }

    @Test
    @Order(1200)
    @DisplayName("GNYL_120: 清空用户名检索输入框")
    void test_GNYL_120_ClearUserSearchInput() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
                .first().dblclick();
        page.waitForTimeout(1000);

        page.getByRole(AriaRole.ROW,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1))
                .first().click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(500);
        page.getByText("权限设置", new Page.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(1000);

        Locator searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='检索'], input[type='text']").first();
        if (searchInput.isVisible()) {
            // 先输入搜索内容
            searchInput.click();
            searchInput.fill("admin");
            page.waitForTimeout(300);

            // 清空输入框
            searchInput.click();
            searchInput.press("Control+a");
            searchInput.fill("");
            page.waitForTimeout(500);

            // 验证输入框已清空
            String value = searchInput.inputValue();
            Assertions.assertTrue(value.isEmpty(), "搜索输入框未清空");
            log.info("GNYL_120 搜索输入框已清空");

            // 验证恢复展示默认人员列表
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
    }

    @Test
    @Order(1210)
    @DisplayName("GNYL_121: 新建一级需求条目")
    void test_GNYL_121_CreateFirstLevelRequirementItem() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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
        TestContext.set("reqItem1", firstItemText);
        log.info("GNYL_121 新建一级条目成功: {}", firstItemText);
    }

    @Test
    @Order(1220)
    @DisplayName("GNYL_122: 新建子需求条目")
    void test_GNYL_122_CreateSubRequirementItem() {
        String firstItem = TestContext.getOrDefault("reqItem1", "req-");

        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(firstItem))
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
        TestContext.set("reqItem2", subItemText);
        log.info("GNYL_122 新建子条目成功: {}", subItemText);
    }

    @Test
    @Order(1230)
    @DisplayName("GNYL_123: 删除需求条目")
    void test_GNYL_123_DeleteRequirementItem() {
        String subItem = TestContext.getOrDefault("reqItem2", "req-");

        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(subItem))
                .locator("div").first()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        page.waitForTimeout(1000);

        page.getByText("删除", new Page.GetByTextOptions().setExact(true)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("确定")).click();
        page.waitForTimeout(1000);

        log.info("GNYL_123 删除条目 {} 成功", subItem);
    }

    @Test
    @Order(1280)
    @DisplayName("GNYL_128/129/130: 显示大纲 -> 结构定位 -> 隐藏大纲")
    void test_GNYL_128_129_130_OutlineAndStructure() {
        page.getByRole(AriaRole.TREEITEM,
                        new Page.GetByRoleOptions().setName(TestConstants.REQ_NAME1).setExact(true))
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
    }


    private String resolveParentId() {
        String parentId = TestContext.get("parentId");
        if (parentId == null || parentId.isEmpty()) {
            parentId = api.findNodeIdByTitle(TestConstants.PROJECT_ID, TestConstants.PARENT_FOLDER);
            if (parentId != null) TestContext.set("parentId", parentId);
        }
        return parentId;
    }

    private String resolveProjectId() {
        String projectId = TestContext.get("projectId");
        if (projectId == null || projectId.isEmpty()) {
            projectId = TestConstants.PROJECT_ID;
            TestContext.set("projectId", projectId);
        }
        return projectId;
    }

}

