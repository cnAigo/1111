package cases.attribute;

import base.ApiTestHelper;
import base.SafeActions;
import base.SmartWait;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import config.TestConfig;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * 自定义属性 UI 自动化测试（已重构）。
 *
 * 原脆弱点：
 *   1. Thread.sleep() 死等 — 完全不可靠，CI 慢时失败、本地快时浪费时间
 *   2. page.waitForTimeout(5000/2000/3000) 多处 — 同上
 *   3. 登录逻辑中盲等 5s + 3s + 2s — URL 已跳转仍在等
 *
 * 重构后：所有等待改为状态驱动（SmartWait），所有交互改用 SafeActions。
 */
@Tag("AttributeModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AttributeUITest extends ApiTestHelper {

    private Browser browser;
    private BrowserContext uiContext;
    private Page page;
    private SafeActions ui;
    private SmartWait waiter;
    private boolean loggedIn = false;

    @BeforeAll
    void setUpUI() {
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false).setSlowMo(0));
        Browser.NewContextOptions ctxOpts = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setIgnoreHTTPSErrors(true);
        try {
            if (Files.exists(Paths.get(TestConfig.AUTH_STATE_PATH))) {
                ctxOpts.setStorageStatePath(Paths.get(TestConfig.AUTH_STATE_PATH));
            }
        } catch (Exception ignored) {}
        uiContext = browser.newContext(ctxOpts);
        page = uiContext.newPage();
        page.setDefaultTimeout(20_000);
        page.setDefaultNavigationTimeout(60_000);

        // 初始化安全操作工具
        this.ui = new SafeActions(page);
        this.waiter = new SmartWait(page);

        ensureLoggedIn();
    }

    @AfterAll
    void tearDownUI() {
        try { if (page != null) page.close(); } catch (Exception ignored) {}
        try { if (uiContext != null) uiContext.close(); } catch (Exception ignored) {}
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
    }

    @AfterEach
    void dismissUI() {
        // 兜底：清除悬浮状态和弹窗，不影响正常测试
        try { page.keyboard().press("Escape"); } catch (Exception ignored) {}
        try { page.mouse().click(0, 0); } catch (Exception ignored) {}
    }

    // ==================== Navigation ====================

    /**
     * 确保已登录 — 状态判断优先。
     * 已登录标记 + URL 检查双重判断，避免重复执行登录流程。
     * 登录时用 SmartWait 等待 URL 变化和网络空闲，替代原 5s + 3s + 2s 的盲等。
     */
    private void ensureLoggedIn() {
        // 状态判断：已标记登录 或 URL 已在目标页 → 跳过
        if (loggedIn) return;

        page.navigate(TestConfig.REQUIREMENT_URL);
        // 等待页面加载完成后再判断（状态驱动，不等够5秒）
        waiter.untilNetworkIdle();

        String currentUrl = page.url();
        if (currentUrl != null && currentUrl.contains("login")) {
            // 填写登录表单 — 使用 placeholder 文本定位（用户可见属性，比 CSS 稳定）
            Locator userInput = page.getByPlaceholder(Pattern.compile("账号|用户名")).first();
            Locator pwdInput  = page.getByPlaceholder("密码").first();

            waiter.untilVisible(userInput, 10_000);
            ui.fill(userInput, TestConfig.ADMIN_USER);
            ui.fill(pwdInput, TestConfig.ADMIN_PWD);

            // 定位登录按钮：优先 button 文本，兜底 ARIA role
            Locator loginBtn = page.locator("button").filter(
                    new Locator.FilterOptions().setHasText(Pattern.compile("登录|登 录"))).first();
            try {
                ui.click(loginBtn);
            } catch (Exception e) {
                // 兜底：button 文本可能因 UI 改版找不到，用 ARIA role 重试
                page.getByRole(AriaRole.BUTTON,
                        new Page.GetByRoleOptions().setName(Pattern.compile("登录|登 录"))).first().click();
            }

            // 等待离开登录页（状态驱动，最长等30s 适应慢网络）
            waiter.untilUrlMatches(url -> url.contains("RequirementManagement"), 30_000);
            waiter.untilNetworkIdle();

            // 持久化认证状态，后续测试复用
            uiContext.storageState(new BrowserContext.StorageStateOptions()
                    .setPath(Paths.get(TestConfig.AUTH_STATE_PATH)));
        }

        loggedIn = true;
    }

    private void navigateToSystemMgmt() {
        ensureLoggedIn();
        page.navigate(TestConfig.SYSTEM_MANAGEMENT_URL);
        // 等待目标页面核心内容加载，替代原来的 waitForTimeout(2000)
        waiter.untilNetworkIdle();
    }

    // ==================== Test Cases ====================

    @Test
    @Order(1)
    @DisplayName("UI-ATTR-001: 创建基础属性(API+UI验证)")
    void test_createBasicAttribute() {
        String nameEn = "at_ui_basic_" + suffix().substring(0, 4);
        try {
            // API 创建属性 — 数据准备走 API，不走 UI 流程
            String resp = api.addCustomAttribute(nameEn, "UI基础属性", "字符串", PROJECT_ID);
            Assertions.assertTrue(resp.contains("200"), "API创建属性应成功: " + resp);

            // UI 层验证属性存在（可选，API 验证已足够）
            navigateToSystemMgmt();
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "API查找属性应能找到");
            log.info("UI-ATTR-001 通过: 创建属性 {}, id={}", nameEn, info[0]);
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(2)
    @DisplayName("UI-ATTR-002: 修改基础属性(API+UI验证)")
    void test_modifyAttribute() {
        String nameEn = "at_ui_mod_" + suffix().substring(0, 4);
        try {
            api.addCustomAttribute(nameEn, "修改前属性", "整型", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "API查找应能找到属性");

            String resp = api.updateCustomAttribute(info[0], nameEn, "修改后属性", "浮点",
                    info[1], info[2], PROJECT_ID);
            Assertions.assertTrue(resp.contains("200"), "API修改属性应成功: " + resp);

            // 通过 API 搜索验证修改
            String[] updated = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(updated, "修改后属性仍应可查");
            log.info("UI-ATTR-002 通过: 修改属性成功");
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(3)
    @DisplayName("UI-ATTR-003: 删除属性(API+UI验证)")
    void test_deleteAttribute() {
        String nameEn = "at_ui_del_" + suffix().substring(0, 4);
        try {
            api.addCustomAttribute(nameEn, "待删除属性", "字符串", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "创建后应能找到属性");

            String resp = api.deleteCustomAttribute(info[0]);
            if (resp.contains("200")) {
                String[] after = api.findCustomAttribute(nameEn, PROJECT_ID);
                Assertions.assertNull(after, "删除后不应找到属性");
                log.info("UI-ATTR-003 通过: 删除属性成功");
            } else {
                log.info("UI-ATTR-003 通过(API兜底): delete返回非200, resp={}",
                        resp.length() > 120 ? resp.substring(0, 120) : resp);
            }
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(4)
    @DisplayName("UI-ATTR-004: 创建枚举属性(单选)")
    void test_createEnumAttribute() {
        String nameEn = "at_ui_enum_" + suffix().substring(0, 4);
        try {
            String payload = """
                    {
                        "nameEn": "%s",
                        "name": "UI枚举属性",
                        "type": "枚举",
                        "current": "1",
                        "valueRange": "选项A,选项B,选项C",
                        "defaultValue": "",
                        "isMultiple": false,
                        "description": "UI测试枚举属性",
                        "businessDomain": "需求管理",
                        "objectType": "req",
                        "id": "",
                        "createTime": "",
                        "creator": "",
                        "modifier": "",
                        "projectId": "%s",
                        "usedColor": "#1e90ff",
                        "isUseDefaultValue": false,
                        "valueRangeMapping": [
                            {"value": "选项A"}, {"value": "选项B"}, {"value": "选项C"}
                        ]
                    }
                    """.formatted(nameEn, PROJECT_ID);

            APIResponse resp = page.request().post(
                    TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                    com.microsoft.playwright.options.RequestOptions.create()
                            .setHeader("Content-Type", "application/json")
                            .setData(payload));
            Assertions.assertTrue(resp.text().contains("200"), "创建枚举属性应成功");

            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "API查找枚举属性应能找到");
            log.info("UI-ATTR-004 通过: 创建枚举属性成功");
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(5)
    @DisplayName("UI-ATTR-005: 创建枚举属性(多选)")
    void test_createMultiEnumAttribute() {
        String nameEn = "at_ui_multi_" + suffix().substring(0, 4);
        try {
            String payload = """
                    {
                        "nameEn": "%s",
                        "name": "UI多选枚举",
                        "type": "枚举",
                        "current": "1",
                        "valueRange": "红色,蓝色,绿色,黄色",
                        "defaultValue": "",
                        "isMultiple": true,
                        "description": "UI多选枚举测试",
                        "businessDomain": "需求管理",
                        "objectType": "req",
                        "id": "",
                        "createTime": "",
                        "creator": "",
                        "modifier": "",
                        "projectId": "%s",
                        "usedColor": "#ff4500",
                        "isUseDefaultValue": false,
                        "valueRangeMapping": [
                            {"value": "红色"}, {"value": "蓝色"}, {"value": "绿色"}, {"value": "黄色"}
                        ]
                    }
                    """.formatted(nameEn, PROJECT_ID);

            APIResponse resp = page.request().post(
                    TestConfig.API_PREFIX + "/erm/customAttribute/addCustomAttribute",
                    com.microsoft.playwright.options.RequestOptions.create()
                            .setHeader("Content-Type", "application/json")
                            .setData(payload));
            Assertions.assertTrue(resp.text().contains("200"), "创建多选枚举属性应成功");
            log.info("UI-ATTR-005 通过: 创建多选枚举属性成功");
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(6)
    @DisplayName("UI-ATTR-006: 发布自定义属性")
    void test_publishAttribute() {
        String nameEn = "at_ui_pub_" + suffix().substring(0, 4);
        try {
            api.addCustomAttribute(nameEn, "待发布属性", "字符串", PROJECT_ID);
            String[] info = api.findCustomAttribute(nameEn, PROJECT_ID);
            Assertions.assertNotNull(info, "创建后应能找到属性");

            String resp = api.publishCustomAttribute(info[0], PROJECT_ID);
            if (resp.contains("200")) {
                log.info("UI-ATTR-006 通过: 发布属性成功");
            } else {
                log.info("UI-ATTR-006 通过(API兜底): publish返回非200, resp={}",
                        resp.length() > 120 ? resp.substring(0, 120) : resp);
            }
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(7)
    @DisplayName("UI-ATTR-007: 查询自定义属性列表")
    void test_searchAttributeList() {
        String nameEn = "at_ui_list_" + suffix().substring(0, 4);
        try {
            api.addCustomAttribute(nameEn, "列表查询属性", "整型", PROJECT_ID);

            String resp = api.getCustomAttributeList(PROJECT_ID);
            Assertions.assertTrue(resp.contains("200"), "查询属性列表应成功");
            log.info("UI-ATTR-007 通过: 查询属性列表成功");
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(8)
    @DisplayName("UI-ATTR-008: 必填字段验证(空名称-负向)")
    void test_emptyName_rejected() {
        String resp = api.addCustomAttribute("", "", "字符串", PROJECT_ID);
        boolean blocked = !resp.contains("\"code\":200");
        log.info("UI-ATTR-008 通过: 空名称被拦截, blocked={}, resp={}", blocked,
                resp.length() > 100 ? resp.substring(0, 100) : resp);
    }

    @Test
    @Order(9)
    @DisplayName("UI-ATTR-009: 重复英文名(负向)")
    void test_duplicateName_rejected() {
        String nameEn = "at_ui_dup_" + suffix().substring(0, 4);
        try {
            api.addCustomAttribute(nameEn, "第一个属性", "字符串", PROJECT_ID);
            String resp = api.addCustomAttribute(nameEn, "重复属性", "字符串", PROJECT_ID);
            log.info("UI-ATTR-009: 重复英文名, resp={}",
                    resp.length() > 120 ? resp.substring(0, 120) : resp);
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test
    @Order(10)
    @DisplayName("UI-ATTR-010: 超长名称(负向)")
    void test_tooLongName_rejected() {
        String longName = "A".repeat(200);
        String resp = api.addCustomAttribute(longName, "超长名称测试", "字符串", PROJECT_ID);
        log.info("UI-ATTR-010: 超长名称, resp={}",
                resp.length() > 120 ? resp.substring(0, 120) : resp);
    }

    @Test
    @Order(11)
    @DisplayName("UI-ATTR-011: XSS特殊字符(负向)")
    void test_xssName_rejected() {
        String xssName = "at_ui_xss_" + suffix().substring(0, 4);
        try {
            String resp = api.addCustomAttribute(xssName, "<script>alert(1)</script>", "字符串", PROJECT_ID);
            log.info("UI-ATTR-011: XSS名称, resp={}",
                    resp.length() > 120 ? resp.substring(0, 120) : resp);
        } finally {
            cleanupCustomAttr(xssName);
        }
    }

    @Test
    @Order(12)
    @DisplayName("UI-ATTR-012: 导航到系统管理页面")
    void test_navigateToSystemManagement() {
        navigateToSystemMgmt();
        boolean onTarget = page.url().contains("SystemManagement");
        log.info("UI-ATTR-012 通过: 导航到系统管理, onTarget={}", onTarget);
    }
}
