package org.example.testvue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import org.example.testvue.entity.TestCaseStep;
import org.example.testvue.repository.TestCaseStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AutomationEngineService {

    private static final String AUTH_STATE_PATH = "auth.json";

    @Autowired
    private TestCaseStepRepository stepRepository;

    @Autowired
    private BrowserAgentService browserAgentService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==========================================
    // 全局登录状态缓存
    // ==========================================

    /**
     * Perform login once and persist browser state (cookies, localStorage, etc.)
     * to {@value #AUTH_STATE_PATH}. Subsequent runs reuse this state to skip login.
     */
    public void generateGlobalLoginState(String loginUrl, String username, String password) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                 new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            page.navigate(loginUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Fill credentials — targets common username/password input patterns
            page.locator("input[type='text'], input[placeholder*='用户'], input[placeholder*='账号'], input[name*='user'], input[name*='account']")
                .first().fill(username);
            page.locator("input[type='password'], input[placeholder*='密码']")
                .first().fill(password);

            // Click login button
            page.locator("button:has-text('登录'), button:has-text('登 录'), button:has-text('Login'), input[type='submit']")
                .first().click();

            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);

            context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(Paths.get(AUTH_STATE_PATH)));

            System.out.println("全局登录状态已保存至: " + AUTH_STATE_PATH);

        } catch (Exception e) {
            throw new RuntimeException("生成全局登录状态失败: " + e.getMessage(), e);
        }
    }

    // ==========================================
    // 模式 1：AI 探索与录制模式 (Record)
    // ==========================================

    @Transactional
    public void runAndRecord(Long testCaseId, List<String> instructions) {
        stepRepository.deleteByTestCaseId(testCaseId);

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                 new BrowserType.LaunchOptions().setHeadless(false));
             BrowserContext context = createContext(browser);
             Page page = context.newPage()) {

            int order = 1;
            for (String instruction : instructions) {
                System.out.println("正在录制步骤 " + order + ": " + instruction);

                try {
                    TestCaseStep step = new TestCaseStep();
                    step.setTestCaseId(testCaseId);
                    step.setOriginalInstruction(instruction);

                    // Auto-detect login page before processing instruction
                    if (isLoginPage(page)) {
                        System.out.println("  检测到登录页面，自动填写凭证...");
                        autoLogin(page);
                    }

                    // --- A. 拦截导航指令 (不经过AI) ---
                    if (instruction.contains("访问") || instruction.startsWith("http")) {
                        String url = extractUrl(instruction);
                        page.navigate(url);
                        page.waitForLoadState(LoadState.NETWORKIDLE);

                        step.setActionType("goto");
                        step.setInputValue(url);
                    }
                    // --- B. 拦截断言指令 (不经过AI) ---
                    else if (instruction.contains("断言") || instruction.contains("跳转至")) {
                        String targetUrl = extractUrl(instruction);
                        page.waitForURL("**" + targetUrl + "**",
                            new Page.WaitForURLOptions().setTimeout(5000));

                        step.setActionType("assert");
                        step.setInputValue(targetUrl);
                    }
                    // --- C. UI 交互指令，交给 AI 处理 ---
                    else {
                        String aiRawResponse = browserAgentService.callAiForStep(page, instruction);
                        String cleanJson = extractJson(aiRawResponse);
                        JsonNode command = objectMapper.readTree(cleanJson);

                        String action = command.get("action").asText();
                        String selector = command.get("selector").asText();
                        String value = command.has("value") ? command.get("value").asText() : "";

                        Locator target = page.locator(selector).first();
                        if ("click".equals(action)) {
                            target.click(new Locator.ClickOptions().setTimeout(5000));
                        } else if ("right_click".equals(action)) {
                            target.click(new Locator.ClickOptions()
                                .setButton(MouseButton.RIGHT).setTimeout(5000));
                        } else if ("fill".equals(action)) {
                            target.fill(value, new Locator.FillOptions().setTimeout(5000));
                        } else {
                            throw new IllegalArgumentException("不支持的动作类型: " + action);
                        }

                        step.setActionType(action);
                        step.setSelector(selector);
                        step.setInputValue(value);
                    }

                    // 执行成功才保存，失败不落库
                    step.setStepOrder(order);
                    stepRepository.save(step);
                    order++;
                    page.waitForLoadState(LoadState.NETWORKIDLE);

                } catch (Exception e) {
                    System.err.println("步骤 " + order + " 执行失败，跳过录制: " + e.getMessage());
                }
            }
            System.out.println("==== 录制完成，共保存 " + (order - 1) + " 个步骤 ====");

        } catch (Exception e) {
            throw new RuntimeException("录制失败，浏览器或环境异常", e);
        }
    }

    // ==========================================
    // 模式 2：纯代码极速回放模式 (Replay - 无AI参与)
    // ==========================================

    public boolean replayTestCase(Long testCaseId) {
        List<TestCaseStep> steps = stepRepository.findByTestCaseIdOrderByStepOrderAsc(testCaseId);
        if (steps.isEmpty()) {
            System.out.println("该用例没有录制的步骤！");
            return false;
        }

        System.out.println("==== 开始极速回放，脱离 AI，纯净执行 ====");
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                 new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext context = createContext(browser);
             Page page = context.newPage()) {

            for (TestCaseStep step : steps) {
                System.out.println(">> 回放第 " + step.getStepOrder() + " 步 ["
                    + step.getActionType() + "]: " + step.getOriginalInstruction());

                switch (step.getActionType()) {
                    case "goto" -> page.navigate(step.getInputValue());
                    case "click" -> page.locator(step.getSelector())
                        .click(new Locator.ClickOptions().setTimeout(5000));
                    case "right_click" -> page.locator(step.getSelector())
                        .click(new Locator.ClickOptions()
                            .setButton(MouseButton.RIGHT).setTimeout(5000));
                    case "fill" -> page.locator(step.getSelector())
                        .fill(step.getInputValue(), new Locator.FillOptions().setTimeout(5000));
                    case "assert" -> {
                        page.waitForURL("**" + step.getInputValue() + "**",
                            new Page.WaitForURLOptions().setTimeout(5000));
                        System.out.println("   [断言成功] 页面已跳转至目标 URL");
                    }
                    default -> throw new IllegalArgumentException(
                        "未知的动作类型: " + step.getActionType());
                }

                page.waitForLoadState(LoadState.NETWORKIDLE);
            }

            System.out.println("==== 回放成功，测试用例通过！ ====");
            return true;

        } catch (Exception e) {
            System.err.println("X 回放失败，测试用例未通过: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // 模式 3：定时调度 (Schedule)
    // ==========================================

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyRegressionTest() {
        System.out.println("触发定时任务：每日回归测试开始...");
        Long loginTestCaseId = 1L;

        boolean isSuccess = replayTestCase(loginTestCaseId);
        if (!isSuccess) {
            System.err.println("【告警】线上环境登录用例巡检失败！请检查系统可用性。");
        }
    }

    // ==========================================
    // 辅助方法
    // ==========================================

    // ── Login page detection & auto-fill ──
    private boolean isLoginPage(Page page) {
        String url = page.url().toLowerCase();
        if (url.contains("/login") || url.contains("/auth") || url.contains("/signin")) {
            System.out.println("[isLoginPage] URL matched: " + url);
            return true;
        }
        try {
            String bodyText = page.textContent("body");
            if (bodyText != null) {
                boolean hasLoginWord = bodyText.contains("登录") || bodyText.contains("Log In") || bodyText.contains("Sign in");
                boolean hasCredField = bodyText.contains("用户名") || bodyText.contains("账号") || bodyText.contains("密码")
                    || bodyText.contains("Username") || bodyText.contains("Password");
                if (hasLoginWord && hasCredField) {
                    System.out.println("[isLoginPage] body text matched");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("[isLoginPage] body text check failed: " + e.getMessage());
        }
        try {
            Object result = page.evaluate("() => {"
                + "const inputs = document.querySelectorAll('.el-input__inner, input');"
                + "let hasInput = false;"
                + "for (const el of inputs) {"
                + "  const ph = (el.placeholder || ''); const nm = (el.name || ''); const tp = (el.type || '');"
                + "  if (ph.includes('用户') || ph.includes('账号') || ph.includes('密码') || ph.includes('user')"
                + "   || nm.includes('user') || nm.includes('account') || nm.includes('password')"
                + "   || tp === 'password') { hasInput = true; break; }"
                + "}"
                + "const btns = document.querySelectorAll('.el-button, button, [role=button], span');"
                + "let isLoginBtn = false;"
                + "for (const b of btns) {"
                + "  const t = (b.textContent || '');"
                + "  if (t.includes('登录') || t.includes('登 录') || t.includes('Login') || t.includes('Sign in'))"
                + "   { isLoginBtn = true; break; }"
                + "}"
                + "return hasInput && isLoginBtn;"
                + "}");
            if (Boolean.TRUE.equals(result)) { System.out.println("[isLoginPage] JS element matched"); return true; }
        } catch (Exception e) {
            System.out.println("[isLoginPage] JS check failed: " + e.getMessage());
        }
        return false;
    }

    private void autoLogin(Page page) {
        String userSelector = ".el-input__inner[type=text], input[type=text], input[placeholder*='用户'], input[placeholder*='账号'], input[placeholder*='用户名'], input[name*='user'], input[name*='account']";
        String pwdSelector  = ".el-input__inner[type=password], input[type=password], input[placeholder*='密码']";
        String btnSelector  = ".el-button:has-text('登录'), .el-button:has-text('登 录'), button:has-text('登录'), button:has-text('登 录'), button:has-text('Login')";
        try {
            page.locator(userSelector).first().fill("admin");
            page.locator(pwdSelector).first().fill("Aa123456");
            page.locator(btnSelector).first().click(new Locator.ClickOptions().setTimeout(5000));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);
            System.out.println("  [AUTO-LOGIN] 已自动登录");
        } catch (Exception e) {
            System.err.println("  [AUTO-LOGIN] 自动登录失败: " + e.getMessage());
        }
    }

    /**
     * Create a BrowserContext, loading saved auth state if available.
     */
    private BrowserContext createContext(Browser browser) {
        Path authPath = Paths.get(AUTH_STATE_PATH);
        if (Files.exists(authPath)) {
            System.out.println("检测到登录状态文件，跳过登录流程: " + AUTH_STATE_PATH);
            return browser.newContext(
                new Browser.NewContextOptions().setStorageStatePath(authPath));
        }
        return browser.newContext();
    }

    private String extractUrl(String text) {
        Pattern p = Pattern.compile("https?://\\S+");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group();
        return text;
    }

    /**
     * Strip Markdown fences and extract the AI command JSON from noisy response text.
     * Iterates all {"action":...} blocks and keeps the last one (the real command),
     * then falls back to the last { ... } pair in the text.
     */
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("AI response is null or empty");
        }
        String text = raw.trim();

        // Strip ```json / ``` fences
        text = text.replaceAll("(?s)^```(?:json)?\\s*", "");
        text = text.replaceAll("(?s)\\s*```$", "");

        // Find all {"action":...} blocks — keep the last one
        java.util.regex.Pattern targetPattern = java.util.regex.Pattern.compile(
            "(?s)\\{\\s*\"action\"\\s*:.*?\\}");
        java.util.regex.Matcher targetMatcher = targetPattern.matcher(text);
        String extractedJson = null;
        while (targetMatcher.find()) extractedJson = targetMatcher.group(0);
        if (extractedJson != null) return extractedJson;

        // Fallback: last { ... } pair
        int lastEndIndex = text.lastIndexOf('}');
        if (lastEndIndex != -1) {
            int matchingStartIndex = text.lastIndexOf('{', lastEndIndex);
            if (matchingStartIndex != -1)
                return text.substring(matchingStartIndex, lastEndIndex + 1);
        }

        throw new RuntimeException(
            "解析失败：未能从 AI 响应中提取出合法的 JSON 对象。原始响应截断内容: \n"
            + text.substring(0, Math.min(text.length(), 200)) + "...");
    }
}
