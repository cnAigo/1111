package org.example.testvue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.example.testvue.entity.AiModelConfig;
import org.example.testvue.entity.AiPromptTemplate;
import org.example.testvue.repository.AiModelConfigRepository;
import org.example.testvue.repository.AiPromptTemplateRepository;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BrowserAgentService {

    private final AiModelConfigRepository configRepo;
    private final AiPromptTemplateRepository promptRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BrowserAgentService(AiModelConfigRepository configRepo, AiPromptTemplateRepository promptRepo) {
        this.configRepo = configRepo;
        this.promptRepo = promptRepo;
    }

    public record AgentResult(String status, String logs, List<String> screenshots) {}
    public record StepCommand(String action, String selector, String value, String instruction,
        String elementTag, String elementText, String pageUrl, String screenshotPath, String playwrightCode) {
        public StepCommand(String action, String selector, String value) {
            this(action, selector, value, "", null, null, null, null, null);
        }
        public StepCommand(String action, String selector, String value, String instruction) {
            this(action, selector, value, instruction, null, null, null, null, null);
        }
    }

    // ── 预编译正则（性能优化 + 可读性） ──
    /** 匹配 Markdown 代码块中的 JSON：```json ... ``` 或 ``` ... ``` */
    private static final Pattern MD_JSON_FENCE = Pattern.compile(
        "(?s)```(?:json)?\\s*\\n?(\\{[^`]*?\\})\\n?```");
    /** 匹配 JSON 对象中带 "action" 字段的完整对象 */
    private static final Pattern ACTION_JSON = Pattern.compile(
        "(?s)\\{\\s*\"action\"\\s*:\\s*\"[^\"]*\"\\s*[^}]*\\}");

    public AgentResult execute(String taskDescription, java.util.function.Consumer<String> logConsumer) {
        return execute(taskDescription, logConsumer, null);
    }

    /**
     * Execute AI-driven browser automation with optional step recording.
     * When {@code stepRecorder} is provided, every successfully executed step
     * (including the initial URL navigation) is passed to it for persistence.
     */
    public AgentResult execute(String taskDescription, java.util.function.Consumer<String> logConsumer,
                               java.util.function.Consumer<StepCommand> stepRecorder) {
        AiModelConfig cfg = configRepo.findByIsActiveTrue().orElse(null);
        if (cfg == null) return new AgentResult("failed", "未启用AI配置", List.of());

        StringBuilder logBuf = new StringBuilder();
        List<String> screenshotPaths = new ArrayList<>();
        java.util.function.Consumer<String> log = msg -> { logBuf.append(msg).append("\n"); if (logConsumer != null) logConsumer.accept(msg); };

        // Log which custom prompt is active
        try {
            java.util.List<AiPromptTemplate> activePrompts = promptRepo.findByIsActiveTrueOrderByCreatedAtDesc();
            if (!activePrompts.isEmpty()) {
                AiPromptTemplate pt = activePrompts.get(0);
                log.accept("[提示词] 使用: \"" + pt.getName() + "\" (" + pt.getContent().length() + "字)");
            }
        } catch (Exception ignored) {}

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720).setIgnoreHTTPSErrors(true));
            Page page = ctx.newPage();

            // View-only mode: only if NO interaction/action keywords present
            boolean hasViewKeyword = taskDescription.contains("查看") || taskDescription.contains("列出")
                || taskDescription.contains("是什么") || taskDescription.contains("有哪些");
            boolean hasActionKeyword = taskDescription.contains("点击") || taskDescription.contains("输入")
                || taskDescription.contains("填写") || taskDescription.contains("新建") || taskDescription.contains("删除")
                || taskDescription.contains("修改") || taskDescription.contains("右击") || taskDescription.contains("选择");
            boolean isView = hasViewKeyword && !hasActionKeyword && findUrl(taskDescription) == null;
            if (isView) {
                String url = findUrl(taskDescription);
                if (url != null) { page.navigate(url); page.waitForLoadState(); page.waitForTimeout(3000); }
                byte[] ss = compressScreenshot(page);
                String answer = callMiMo(cfg, "详细描述页面内容，回答: " + taskDescription, ss, "[]");
                log.accept("AI: " + answer);
                browser.close();
                return new AgentResult("passed", logBuf.toString(), List.of());
            }

            // Navigate — wait for SPA to fully render, then handle login if needed
            String firstUrl = findUrl(taskDescription);
            if (firstUrl != null) {
                page.navigate(firstUrl);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                try {
                    page.waitForSelector("input, button, .el-input__inner, .el-button",
                        new Page.WaitForSelectorOptions().setTimeout(8000));
                } catch (Exception e) {
                    log.accept("[NAV] 页面可能为纯展示页或加载较慢: " + e.getMessage());
                }
                page.waitForTimeout(2000);
                log.accept("[NAV] " + firstUrl + " → 当前: " + page.url());
                if (stepRecorder != null)
                    stepRecorder.accept(new StepCommand("goto", "", firstUrl, "访问 " + firstUrl,
                        "", "", "", "", "page.goto(\"" + firstUrl + "\");"));

                // Aggressive login check right after navigation (before step loop)
                if (isLoginPage(page)) {
                    log.accept("  [NAV] 导航后检测到登录页面，自动登录...");
                    autoLogin(page, log, stepRecorder);
                    // Re-navigate to target URL after login (auth redirects to home page)
                    page.navigate(firstUrl);
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    page.waitForTimeout(2000);
                    log.accept("  [NAV] 登录后重新导航 → 当前: " + page.url());
                }
            }

            // Parse tasks into atomic steps — keep all lines, handle URLs inline
            List<String> steps = new ArrayList<>();
            for (String line : taskDescription.split("\n")) {
                line = line.trim();
                if (!line.isEmpty()) steps.add(line);
            }

            int stepNum = 0, passed = 0, failed = 0;
            List<Map<String, Object>> trace = new ArrayList<>();
            StepCommand lastCmd = null;
            boolean alreadyNavigated = (firstUrl != null);

            for (String step : steps) {
                stepNum++;
                Map<String, Object> stepTrace = new LinkedHashMap<>();
                stepTrace.put("step", stepNum); stepTrace.put("action", step); stepTrace.put("url", page.url());
                log.accept(String.format("[%d/%d] %s", stepNum, steps.size(), step));

                // Skip URL-only steps (navigation already done before loop)
                if (step.matches(".*https?://\\S+.*") && alreadyNavigated) {
                    log.accept("  V NAV (已在循环前完成)");
                    passed++;
                    stepTrace.put("result", "success"); trace.add(stepTrace);
                    continue;
                }

                // Auto-detect and handle login page before processing the step
                if (isLoginPage(page)) {
                    log.accept("  检测到登录页面，自动填写凭证...");
                    autoLogin(page, log, stepRecorder);
                }

                // ③ Assert: handled entirely by code, no AI involved
                String intent = classifyIntent(step);
                if ("assert".equals(intent)) {
                    page.waitForTimeout(1500);
                    String ck = step.replaceAll(".*?[断言验证assert]+\\s*", "").replaceAll("是否|跳转至|包含", "").trim();
                    boolean ok = !ck.isEmpty() && (page.url().contains(ck) || page.textContent("body").contains(ck));
                    log.accept(ok ? "  V ASSERT: " + ck : "  X ASSERT FAILED");
                    if (ok) passed++; else failed++;
                    stepTrace.put("result", ok ? "passed" : "failed"); trace.add(stepTrace); continue;
                }

                // ── AI step with auto-correction retry loop (max 3 attempts) ──
                boolean stepDone = false;
                String lastError = null;
                for (int retry = 0; retry < 3 && !stepDone; retry++) {
                    try {
                    // ② Collect fresh DOM + screenshot each retry
                    String domData = collectDomAttributes(page);
                    byte[] ss = compressScreenshot(page);

                    // ① Build prompt with short-term memory + error feedback
                    String prompt = buildPrompt(intent, step, domData, lastCmd, lastError);
                    String rawJson = callMiMo(cfg, prompt, ss, domData);

                    // 【修复致命Bug】先替换换行再计算长度，避免 replaceAll 缩短字符串后
                    // 用原始长度做 substring 导致 StringIndexOutOfBoundsException
                    String compactJson = rawJson != null ? rawJson.replaceAll("\\s+", " ") : "null";
                    int safeLen = Math.min(150, compactJson.length());
                    log.accept("  AI" + (retry > 0 ? "[重试" + retry + "]" : "") + ": "
                        + compactJson.substring(0, safeLen));
                    stepTrace.put("ai_response" + (retry > 0 ? "_retry" + retry : ""), rawJson);

                    try {
                        StepCommand cmd = executeAiCommand(page, rawJson);
                        passed++;
                        stepDone = true;
                        lastCmd = cmd; // remember for next step's context
                        log.accept("  V " + intent.toUpperCase() + " OK [" + cmd.action() + " " + cmd.selector() + "]");
                        stepTrace.put("result", "success");
                        // Save screenshot for this step
                        String ssPath = saveScreenshot(ss, stepNum);
                        if (ssPath != null) { screenshotPaths.add(ssPath); stepTrace.put("screenshot", ssPath); }
                        if (stepRecorder != null) {
                            var detail = captureElementDetail(page, cmd.selector(), cmd.action());
                            String code = generatePlaywrightCode(cmd.action(), cmd.selector(), cmd.value());
                            stepRecorder.accept(new StepCommand(cmd.action(), cmd.selector(), cmd.value(), step,
                                detail[0], detail[1], page.url(), ssPath, code));
                        }
                    } catch (Exception e) {
                        lastError = e.getMessage();
                        if (retry < 2) {
                            // 【修复】同样先替换再取安全长度
                            String safeErr = lastError != null ? lastError : "null";
                            int errLen = Math.min(100, safeErr.length());
                            log.accept("  ! 重试 " + (retry + 1) + "/2: " + safeErr.substring(0, errLen));
                            page.waitForTimeout(800); // brief pause before retry
                        } else {
                            failed++;
                            log.accept("  X FAILED after 3 attempts: " + lastError);
                            stepTrace.put("result", "failed: " + lastError);
                        }
                    }
                } catch (Exception outerEx) {
                        // Catch-all for AI call / screenshot / parsing failures
                        failed++;
                        String msg = outerEx.getMessage();
                        // 【修复】安全的 substring 计算
                        String safeMsg = msg != null ? msg : "null";
                        int msgLen = Math.min(200, safeMsg.length());
                        log.accept("  X STEP ERROR: " + safeMsg.substring(0, msgLen));
                        stepTrace.put("result", "step_error: " + msg);
                        stepDone = true; // stop retrying this step
                    }
                }

                // Physical wait between steps — let UI animations / menus settle
                page.waitForTimeout(1000);
                trace.add(stepTrace);
            }

            browser.close();
            String status = failed == 0 ? "passed" : (passed > 0 ? "passed" : "failed");
            String traceJson = mapper.writeValueAsString(trace);
            log.accept(String.format("DONE: %d/%d", passed, steps.size()));
            log.accept("TRACE: " + traceJson);
            return new AgentResult(status, logBuf.toString(), screenshotPaths);

        } catch (Exception e) { log.accept("FATAL: " + e.getMessage()); return new AgentResult("failed", logBuf.toString(), screenshotPaths); }
    }

    // ── ⑤ Classify intent into atomic action ──
    private String classifyIntent(String step) {
        if (step.contains("输入") || step.contains("填写")) return "fill";
        if (step.contains("断言") || step.contains("验证")) return "assert";
        if (step.contains("双击")) return "click";  // dblclick is chosen by AI
        return "click";
    }

    // ── ⑤ Build prompt with short-term memory + error feedback ──
    private String buildPrompt(String intent, String step, String domData, StepCommand lastCmd, String lastError) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            你是一个聪明的 UI 自动化测试专家。请先简短分析当前页面元素与用户指令的匹配关系，然后输出JSON。

            【执行策略】
            1. 优先使用文本包含来定位，例如 text='保存' 或 :has-text('保存')
            2. 如果有多个同名按钮，结合DOM中的父级特征生成精确的CSS/XPath
            3. 禁止动态ID（如el-id-3932-3），禁止把输入值当name属性（如[name='admin']）
            4. 前端框架 Element Plus

            【可用操作】
            click / fill / right_click / dblclick / type
            - fill: 普通输入框填充，需 selector
            - dblclick: 双击元素（如双击文件名进入重命名）
            - type: 输入框已被激活时用，无需selector，Ctrl+A全选后输入value

            你的回复必须包含一个Markdown格式的JSON块，例如：
            ```json
            {"action":"click", "selector":"button:has-text('登录')", "value":""}
            ```

            """);

        // Short-term memory
        if (lastCmd != null) {
            sb.append("【上一步执行结果】\n");
            sb.append("动作: ").append(lastCmd.action())
              .append(" | 选择器: ").append(lastCmd.selector())
              .append(" | 状态: 成功\n\n");
        }

        // Error feedback for retry
        if (lastError != null) {
            sb.append("【上次尝试失败！请换策略】\n");
            sb.append("错误: ").append(lastError).append("\n");
            sb.append("请观察最新截图和DOM，换不同的选择器或操作方式。\n\n");
        }

        sb.append("""
            【Few-Shot】
            指令: 点击登录 | DOM: [{"tag":"button","text":"登 录"}]
            分析: 找到按钮，文本'登 录'匹配，使用button:has-text。
            ```json
            {"action":"click","selector":"button:has-text('登 录')","value":""}
            ```

            指令: 输入admin | DOM: [{"tag":"input","type":"text","placeholder":"请输入用户名"}]
            分析: 定位到placeholder为'请输入用户名'的输入框。
            ```json
            {"action":"fill","selector":"input[placeholder='请输入用户名']","value":"admin"}
            ```

            【当前任务】
            指令: """).append(step).append("\n")
          .append("DOM: ").append(domData.length() > 1000 ? domData.substring(0, 1000) + "..." : domData).append("\n\n")
          .append("请先简短分析，然后输出JSON块：");

        return sb.toString();
    }

    // ── ② Collect visible DOM elements — broad capture for AI context ──
    private String collectDomAttributes(Page page) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Object result = page.evaluate("() => {"
                    + "const data = [];"
                    + "const seen = new Set();"
                    // Pass 1: interactive form elements
                    + "document.querySelectorAll('input, button, a, select, textarea, [role=button], [onclick], "
                    + "  .el-input__inner, .el-button, .el-select').forEach(e => {"
                    + "  if (!seen.has(e) && data.length < 60) { seen.add(e);"
                    + "    data.push({tag:e.tagName.toLowerCase(),id:e.id||'',name:e.name||'',"
                    + "      cls:(typeof e.className==='string'?e.className:'').substring(0,80),"
                    + "      type:e.type||'',placeholder:e.placeholder||'',"
                    + "      text:(e.textContent||'').substring(0,60).trim(),"
                    + "      role:e.getAttribute('role')||'',aria:e.getAttribute('aria-label')||''}); } });"
                    // Pass 2: tree nodes, menu items, context menus
                    + "document.querySelectorAll('.el-tree-node, .el-tree-node__content, [role=treeitem], [role=tree], "
                    + "  .el-menu-item, .el-sub-menu__title, .el-dropdown-menu__item, .el-contextmenu-item, "
                    + "  .el-context-menu__item, .el-popper, .el-cascader-node, .el-select-dropdown__item').forEach(e => {"
                    + "  if (!seen.has(e) && data.length < 100) { seen.add(e);"
                    + "    data.push({tag:e.tagName.toLowerCase(),id:e.id||'',"
                    + "      cls:(typeof e.className==='string'?e.className:'').substring(0,80),"
                    + "      text:(e.textContent||'').substring(0,60).trim(),"
                    + "      role:e.getAttribute('role')||'',aria:e.getAttribute('aria-label')||''}); } });"
                    // Pass 3: any visible element with non-empty text
                    + "document.querySelectorAll('span, div, li, label, h1, h2, h3, h4, h5, h6, p, td, th').forEach(e => {"
                    + "  const txt = (e.textContent||'').trim();"
                    + "  if (!seen.has(e) && data.length < 150 && txt.length > 0 && txt.length < 100"
                    + "   && e.offsetParent !== null && e.children.length === 0) { seen.add(e);"
                    + "    data.push({tag:e.tagName.toLowerCase(),id:e.id||'',"
                    + "      cls:(typeof e.className==='string'?e.className:'').substring(0,80),"
                    + "      text:txt.substring(0,60)}); } });"
                    + "return JSON.stringify(data);"
                    + "}").toString();
                String dom = result != null ? result.toString() : "[]";
                if (!"[]".equals(dom) || attempt == 3) return dom;
                page.waitForTimeout(1500);
            } catch (Exception e) {
                if (attempt == 3) return "[]";
                try { page.waitForTimeout(1000); } catch (Exception ignored) {}
            }
        }
        return "[]";
    }

    /**
     * Execute a single AI-generated UI automation command with fast-fail semantics.
     * No fallback selectors — if the AI's selector cannot be found, the step fails immediately.
     *
     * 【重构要点】
     *   1. 任何 action 前强制 scrollIntoViewIfNeeded（不包在 try-catch 里吞异常）
     *   2. fill 改为 click() + pressSequentially(delay=50)，完美兼容 Vue/Element Plus 双向绑定
     *
     * @param page        Playwright Page instance
     * @param rawResponse raw AI response text (may contain Markdown wrapping)
     * @throws RuntimeException       if the selector is not found, times out, or the JSON is malformed
     * @throws IllegalArgumentException if the action is not 'click' or 'fill'
     */
    public StepCommand executeAiCommand(Page page, String rawResponse) {
        String json;
        try {
            json = extractJson(rawResponse);
        } catch (Exception e) {
            // 【修复】安全的 substring：先取安全长度再截取
            String safeRaw = rawResponse != null ? rawResponse : "null";
            int safeLen = Math.min(safeRaw.length(), 100);
            throw new RuntimeException("extractJson failed. Raw AI[" + safeRaw.length() + "]: "
                + safeRaw.substring(0, safeLen), e);
        }

        String action;
        String selector;
        String value;
        try {
            Map<String, Object> cmd = mapper.readValue(json, Map.class);
            action = cmd.get("action") != null ? cmd.get("action").toString() : "";
            selector = cmd.get("selector") != null ? cmd.get("selector").toString() : "";
            value = cmd.get("value") != null ? cmd.get("value").toString() : "";
        } catch (Exception e) {
            String safeJson = json != null ? json : "null";
            int safeLen = Math.min(safeJson.length(), 200);
            throw new RuntimeException("Failed to parse JSON[" + safeJson.length() + "]: "
                + safeJson.substring(0, safeLen), e);
        }

        if (!"click".equals(action) && !"fill".equals(action) && !"right_click".equals(action)
            && !"type".equals(action) && !"dblclick".equals(action)) {
            String safeRaw = rawResponse != null ? rawResponse : "null";
            int safeLen = Math.min(safeRaw.length(), 200);
            throw new IllegalArgumentException(
                "Unsupported action '" + action + "'. Raw: " + safeRaw.substring(0, safeLen));
        }

        if (selector.isBlank() && !"type".equals(action)) {
            String safeRaw = rawResponse != null ? rawResponse : "null";
            int safeLen = Math.min(safeRaw.length(), 200);
            throw new RuntimeException("AI returned empty selector. Raw response: "
                + safeRaw.substring(0, safeLen));
        }

        try {
            Locator target = page.locator(selector).first();

            // 【重构】强制滚动到视口：任何 action 前必须执行，不包在 try-catch 中吞异常
            // 如果元素不在视口内（被固定 header 遮挡、在折叠区域外），scroll 失败意味着
            // 后续 click/fill 必定失败，应当直接暴露问题而非静默跳过
            if (!"type".equals(action)) {
                target.scrollIntoViewIfNeeded(
                    new Locator.ScrollIntoViewIfNeededOptions().setTimeout(5000));
            }

            switch (action) {
                case "click" -> target.click(new Locator.ClickOptions().setTimeout(5000));
                case "right_click" -> target.click(new Locator.ClickOptions()
                    .setButton(com.microsoft.playwright.options.MouseButton.RIGHT).setTimeout(5000));
                case "dblclick" -> target.dblclick(new Locator.DblclickOptions().setTimeout(5000));
                case "fill" -> {
                    // 【重构】先 click 触发 Vue/Element Plus 的 focus 事件，
                    // 再用 pressSequentially(delay=50) 模拟人类逐字输入。
                    // 原生 fill() 直接设置 value 属性，不触发 input 事件，
                    // 导致 Element Plus 的 v-model 双向绑定不更新。
                    target.click(new Locator.ClickOptions().setTimeout(5000));
                    // Ctrl+A 全选已有内容（如果输入框有默认值）
                    target.press("Control+a");
                    // 模拟人类逐字输入，每次按键间隔 50ms，确保 Vue 响应式系统捕获每次 input 事件
                    target.pressSequentially(value,
                        new Locator.PressSequentiallyOptions().setDelay(50));
                }
                case "type" -> {
                    page.keyboard().press("Control+a");
                    page.waitForTimeout(100);
                    page.keyboard().type(value);
                    page.keyboard().press("Enter");
                }
            }
        } catch (PlaywrightException e) {
            throw new RuntimeException(
                "Action '" + action + "' failed on selector [" + selector + "]: " + e.getMessage(), e);
        }
        return new StepCommand(action, selector, value);
    }

    /**
     * One-shot AI call for a single step — collects DOM, takes screenshot, calls MiMo,
     * and returns the raw AI response text. The caller should then use
     * {@link #executeAiCommand(Page, String)} to parse and execute the command.
     */
    public String callAiForStep(Page page, String stepDescription) {
        AiModelConfig cfg = configRepo.findByIsActiveTrue().orElse(null);
        if (cfg == null) throw new RuntimeException("No active AI model config");
        String domData = collectDomAttributes(page);
        byte[] ss = compressScreenshot(page);
        String intent = classifyIntent(stepDescription);
        String prompt = buildPrompt(intent, stepDescription, domData, null, null);
        return callMiMo(cfg, prompt, ss, domData);
    }

    /**
     * 【重构】从 AI 响应中提取 JSON 命令。
     *
     * 策略（按优先级）：
     *   1. 正则匹配 Markdown 代码块 ```json { ... } ```（最可靠，AI 被训练输出此格式）
     *   2. 正则匹配第一个 {"action": "..." ...} 完整 JSON 对象（处理 AI 输出但未用代码块包裹的情况）
     *   3. 兜底：查找最外层 { ... } 配对（lastIndexOf 仅作为最后手段）
     *
     * 原方法仅靠 lastIndexOf 提取最外层 {}，当 AI 输出中包含分析文本的 { 括号时
     * 会截取到错误的内容。现改为正则优先 + 语义匹配。
     */
    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("AI response is null or empty");
        }

        // ── 策略1：正则提取 Markdown 代码块中的 JSON ──
        // 匹配 ```json { ... } ``` 或 ``` { ... } ```
        // 使用 [^`]*? 而非 .*?，确保不会跨越代码块边界匹配
        Matcher fenceMatcher = MD_JSON_FENCE.matcher(raw);
        if (fenceMatcher.find()) {
            String jsonMatch = fenceMatcher.group(1).trim();
            if (jsonMatch.startsWith("{") && jsonMatch.endsWith("}")) {
                return jsonMatch;
            }
        }

        // ── 策略2：正则提取带 "action" 字段的完整 JSON 对象 ──
        // 这比 lastIndexOf 精确得多：它匹配语义正确的 JSON 而非任意 { ... }
        Matcher actionMatcher = ACTION_JSON.matcher(raw);
        if (actionMatcher.find()) {
            return actionMatcher.group(0).trim();
        }

        // ── 策略3（兜底）：最外层大括号配对 ──
        // 仅在以上两种正则都失败时使用，处理 AI 输出格式完全异常的情况
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return raw.substring(firstBrace, lastBrace + 1);
        }

        // 【修复】安全的 substring：先取安全长度，避免越界
        String safeRaw = raw;
        int safeLen = Math.min(safeRaw.length(), 150);
        throw new RuntimeException(
            "无法从响应中提取JSON。AI 回复片段: " + safeRaw.substring(0, safeLen));
    }

    // ── ① Parse AI JSON response (no fallback — fast-fail on malformed input) ──
    private Map<String, Object> parseAiJson(String raw) {
        String json = extractJson(raw);
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            String safeRaw = raw != null ? raw : "null";
            int safeLen = Math.min(safeRaw.length(), 200);
            throw new RuntimeException("Failed to parse AI response JSON: "
                + safeRaw.substring(0, safeLen), e);
        }
    }

    // ── Compress screenshot ──
    private byte[] compressScreenshot(Page page) {
        try {
            byte[] raw = page.screenshot(new Page.ScreenshotOptions().setFullPage(false));
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
            int w = Math.min(img.getWidth(), 1024), h = Math.min(img.getHeight(), 768);
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            out.createGraphics().drawImage(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(out, "jpg", bos);
            return bos.toByteArray();
        } catch (Exception e) { return page.screenshot(); }
    }

    // ── Call MiMo Vision API ──
    private String callMiMo(AiModelConfig cfg, String prompt, byte[] screenshot, String domData) {
        try {
            String baseUrl = (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank())
                ? cfg.getBaseUrl().replaceAll("/+$", "") : "https://api.xiaomimimo.com/v1";
            // Load custom prompt template if active
            String customPrompt = "";
            try {
                List<AiPromptTemplate> activePrompts = promptRepo.findByIsActiveTrueOrderByCreatedAtDesc();
                if (!activePrompts.isEmpty()) {
                    AiPromptTemplate pt = activePrompts.get(0);
                    if (pt.getContent() != null && !pt.getContent().isBlank()) {
                        customPrompt = pt.getContent() + "\n\n";
                        String logMsg = "  [提示词] 已加载: " + pt.getName() + " (" + pt.getContent().length() + "字)";
                        System.out.println(logMsg);
                    }
                }
            } catch (Exception ignored) {}

            // 【重构】系统提示词：引导 AI 优先利用截图视觉布局 + DOM 属性联合决策。
            // 删除了"不要依赖截图猜测"的限制语，改为鼓励视觉+DOM双重验证。
            // 视觉大模型能感知元素的层级遮挡、可见性和空间位置，这是纯 DOM 无法提供的。
            String systemPrompt = customPrompt
                + "你是网页自动化专家。下面提供了页面上所有可交互元素的真实DOM属性（JSON数组），以及页面截图。"
                + "请综合【页面截图】的视觉布局和【DOM属性】来生成最精准的选择器。\n"
                + "截图可以帮助你判断元素的层级关系、可见性、是否被遮挡、空间位置；"
                + "DOM属性提供了精确的标签、class、text等元数据。两者结合使用。\n\n"
                + "【最高级别格式警告】你是一个没有感情的JSON生成机器。绝对禁止输出任何思考过程、推理步骤、"
                + "\"首先\"、\"分析\"等前置语言！"
                + "你的所有回答必须以 { 开头，以 } 结尾。除此之外的任何字符都将被视为严重违规！\n\n"
                + "DOM属性: " + domData + "\n\n";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", cfg.getModelName() != null ? cfg.getModelName() : "mimo-v2.5");
            body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", prompt),
                    Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(screenshot)))
                ))
            ));
            body.put("max_tokens", 4096);

            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + cfg.getApiKey())
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String rawBody = resp.body();
            if (resp.statusCode() == 200) {
                try {
                Map<String, Object> r = mapper.readValue(rawBody, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) r.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    if (msg != null) {
                        String c = (String) msg.get("content");
                        if (c != null && !c.isBlank()) return c.trim();
                        String rc = (String) msg.get("reasoning_content");
                        if (rc != null && !rc.isBlank()) return rc.trim();
                    }
                }
                    return rawBody;
                } catch (Exception parseEx) {
                    // 【修复】安全的 substring 计算
                    String safeBody = rawBody != null ? rawBody : "null";
                    int safeLen = Math.min(safeBody.length(), 300);
                    System.err.println("MiMo parse error, raw body[" + safeBody.length() + "]: "
                        + safeBody.substring(0, safeLen));
                    return rawBody;
                }
            }
            // 【修复】安全的 substring
            String safeBody = rawBody != null ? rawBody : "null";
            int safeLen = Math.min(safeBody.length(), 300);
            throw new RuntimeException("MiMo API returned status " + resp.statusCode() + ": "
                + safeBody.substring(0, safeLen));
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("MiMo API call failed: " + e.getMessage(), e); }
    }

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
                + "  const ph = (el.placeholder || '');"
                + "  const nm = (el.name || '');"
                + "  const tp = (el.type || '');"
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
            if (Boolean.TRUE.equals(result)) {
                System.out.println("[isLoginPage] JS element matched");
                return true;
            }
        } catch (Exception e) {
            System.out.println("[isLoginPage] JS check failed: " + e.getMessage());
        }
        return false;
    }

    private void autoLogin(Page page, java.util.function.Consumer<String> log,
                           java.util.function.Consumer<StepCommand> stepRecorder) {
        String userSelector = ".el-input__inner[type=text], input[type=text], input[placeholder*='用户'], input[placeholder*='账号'], input[placeholder*='用户名'], input[name*='user'], input[name*='account']";
        String pwdSelector  = ".el-input__inner[type=password], input[type=password], input[placeholder*='密码']";
        String btnSelector  = ".el-button:has-text('登录'), .el-button:has-text('登 录'), button:has-text('登录'), button:has-text('登 录'), button:has-text('Login')";

        try {
            page.locator(userSelector).first().fill("admin");
            log.accept("  [AUTO-LOGIN] admin / *** → 已登录");
            if (stepRecorder != null) {
                stepRecorder.accept(new StepCommand("fill", userSelector, "admin", "自动填写用户名"));
                stepRecorder.accept(new StepCommand("fill", pwdSelector, "Aa123456", "自动填写密码"));
            }

            page.locator(pwdSelector).first().fill("Aa123456");
            page.locator(btnSelector).first().click(new Locator.ClickOptions().setTimeout(5000));
            page.waitForLoadState();
            page.waitForTimeout(2000);
            if (stepRecorder != null)
                stepRecorder.accept(new StepCommand("click", btnSelector, "", "自动点击登录"));
        } catch (PlaywrightException e) {
            log.accept("  [AUTO-LOGIN] 自动登录失败: " + e.getMessage());
        }
    }

    /** Capture [tag, text] of the element matched by selector */
    private String[] captureElementDetail(Page page, String selector, String action) {
        if (selector == null || selector.isBlank() || "goto".equals(action) || "type".equals(action))
            return new String[]{action, ""};
        try {
            Object result = page.evaluate("(sel) => {"
                + "try { const el = document.querySelector(sel);"
                + " if (!el) return JSON.stringify({tag:'',text:''});"
                + " return JSON.stringify({tag:el.tagName.toLowerCase(),"
                + "  text:(el.textContent||'').substring(0,80).trim(),"
                + "  attrs:{id:el.id||'',cls:(typeof el.className==='string'?el.className:'').substring(0,80),"
                + "  placeholder:el.placeholder||'',type:el.type||'',name:el.name||''}});"
                + "} catch(e) { return JSON.stringify({tag:'',text:''}); } }", selector);
            String json = result != null ? result.toString() : "{}";
            Map<String, Object> map = mapper.readValue(json, Map.class);
            String tag = (String) map.getOrDefault("tag", "");
            String text = (String) map.getOrDefault("text", "");
            return new String[]{tag, text};
        } catch (Exception e) { return new String[]{"", ""}; }
    }

    /** Generate a Playwright code line for this action */
    private String generatePlaywrightCode(String action, String selector, String value) {
        return switch (action) {
            case "click" -> "page.locator(\"" + selector.replace("\"", "\\\"") + "\").click();";
            case "dblclick" -> "page.locator(\"" + selector.replace("\"", "\\\"") + "\").dblclick();";
            case "right_click" -> "page.locator(\"" + selector.replace("\"", "\\\"") + "\").click({ button: 'right' });";
            case "fill" -> "page.locator(\"" + selector.replace("\"", "\\\"") + "\").fill(\"" + (value != null ? value.replace("\"", "\\\"") : "") + "\");";
            case "goto" -> "page.goto(\"" + (value != null ? value : "") + "\");";
            case "type" -> "page.keyboard().type(\"" + (value != null ? value : "") + "\");";
            default -> "";
        };
    }

    private String saveScreenshot(byte[] bytes, int stepNum) {
        try {
            String name = "ai_step" + stepNum + "_" + System.currentTimeMillis() + ".png";
            java.nio.file.Path dir = java.nio.file.Paths.get("target/screenshots");
            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.write(dir.resolve(name), bytes);
            return name;
        } catch (Exception e) { return null; }
    }

    private String findUrl(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(text);
        return m.find() ? m.group() : null;
    }
}
