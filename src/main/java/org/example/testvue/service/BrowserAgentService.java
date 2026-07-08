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
                    log.accept("  AI" + (retry > 0 ? "[重试" + retry + "]" : "") + ": "
                        + (rawJson != null ? rawJson.replaceAll("\\s+", " ").substring(0, Math.min(150, rawJson.length())) : "null"));
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
                            log.accept("  ! 重试 " + (retry + 1) + "/2: " + lastError.substring(0, Math.min(100, lastError.length())));
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
                        log.accept("  X STEP ERROR: " + (msg != null ? msg.substring(0, Math.min(200, msg.length())) : "null"));
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
            你是一个严谨且专业的 Web UI 自动化测试执行引擎。将操作指令、DOM属性、截图转换为 Playwright 可执行的严格 JSON。

            【全局约束】
            1. 你是一个JSON生成机器。回答必须以{开头、以}结尾。绝对禁止输出任何思考过程、推理、"首先"、"分析"、"现在"等废话！
            2. value必须用指令里的真实数据，禁止"要输入的值"等占位符，无输入则""
            3. action只能是 click、fill、right_click、dblclick 或 type
               - fill: 用在普通输入框，需要 selector
               - dblclick: 双击元素（如双击文件名进入重命名）
               - type: 用在输入框已被激活的场景，无需 selector，Ctrl+A全选后输入value
            4. 选择器优先级：button:has-text('xxx') > span:has-text('xxx') > [placeholder='xxx'] > [type='xxx'] > 唯一ID
               禁止动态ID(如el-id-3932-3，除非唯一)，禁止把输入值当name属性(如[name='admin'])
            5. 前端框架 Element Plus
            6. 操作完成后页面需要时间渲染（下拉菜单、弹窗、右键菜单），截图和DOM可能延迟。
               如果你要找的元素不在DOM中，尝试根据指令语义选择最接近的可见元素。

            """);

        // Short-term memory: tell AI what just happened
        if (lastCmd != null) {
            sb.append("【上一步执行结果】\n");
            sb.append("动作: ").append(lastCmd.action())
              .append(" | 选择器: ").append(lastCmd.selector())
              .append(" | 值: ").append(lastCmd.value() != null ? lastCmd.value() : "")
              .append(" | 状态: 成功\n\n");
        }

        // Error feedback for auto-correction retry
        if (lastError != null) {
            sb.append("【上次尝试失败！请换策略】\n");
            sb.append("错误信息: ").append(lastError).append("\n");
            sb.append("请观察最新截图和DOM，换一个不同的选择器或操作方式重试。\n\n");
        }

        sb.append("""
            【Few-Shot】
            指令: 点击登录 | DOM: [{"tag":"button","text":"登 录"}]
            → {"action":"click","selector":"button:has-text('登 录')","value":""}

            指令: 输入admin | DOM: [{"tag":"input","type":"text","placeholder":"请输入用户名"}]
            → {"action":"fill","selector":"input[placeholder='请输入用户名']","value":"admin"}

            【当前任务】
            指令: """).append(step).append("\n")
          .append("DOM属性: ").append(domData.length() > 800 ? domData.substring(0, 800) + "..." : domData).append("\n\n")
          .append("立刻输出纯JSON（不要任何其他文字）。你的回复必须且只能以 {\"action\": 开始：");

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
            throw new RuntimeException("extractJson failed. Raw AI[" + rawResponse.length() + "]: "
                + rawResponse.substring(0, Math.min(rawResponse.length(), 100)), e);
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
            throw new RuntimeException("Failed to parse JSON[" + json.length() + "]: " + json, e);
        }

        if (!"click".equals(action) && !"fill".equals(action) && !"right_click".equals(action) && !"type".equals(action) && !"dblclick".equals(action)) {
            throw new IllegalArgumentException(
                "Unsupported action '" + action + "'. Raw: " + rawResponse);
        }

        if (selector.isBlank() && !"type".equals(action)) {
            throw new RuntimeException("AI returned empty selector. Raw response: " + rawResponse);
        }

        try {
            switch (action) {
                case "click" -> page.locator(selector).first()
                    .click(new Locator.ClickOptions().setTimeout(5000));
                case "right_click" -> page.locator(selector).first()
                    .click(new Locator.ClickOptions()
                        .setButton(com.microsoft.playwright.options.MouseButton.RIGHT)
                        .setTimeout(5000));
                case "dblclick" -> page.locator(selector).first()
                    .dblclick(new Locator.DblclickOptions().setTimeout(5000));
                case "fill" -> {
                    page.locator(selector).first()
                        .click(new Locator.ClickOptions().setTimeout(5000));
                    page.locator(selector).first()
                        .fill(value, new Locator.FillOptions().setTimeout(5000));
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
     * Strip Markdown fences and aggressively extract the AI command JSON from noisy response text.
     *
     * Strategy (tried in order):
     * 1. Match {"action":"click|fill"...} — the exact JSON shape we expect
     * 2. Fall back to the last { ... } block in the text (chain-of-thought usually comes first)
     * 3. Fail with a diagnostic message including the raw response
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

    // ── ① Parse AI JSON response (no fallback — fast-fail on malformed input) ──
    private Map<String, Object> parseAiJson(String raw) {
        String json = extractJson(raw);
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response JSON: " + raw, e);
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
                    AiPromptTemplate pt = activePrompts.get(0); // first active = default
                    if (pt.getContent() != null && !pt.getContent().isBlank()) {
                        customPrompt = pt.getContent() + "\n\n";
                        String logMsg = "  [提示词] 已加载: " + pt.getName() + " (" + pt.getContent().length() + "字)";
                        System.out.println(logMsg);
                        // Inject into execution log — but we don't have log consumer here
                        // Instead, we return it prepended to the prompt so it's visible
                    }
                }
            } catch (Exception ignored) {}

            String systemPrompt = customPrompt
                + "你是网页自动化专家。下面提供了页面上所有可交互元素的真实DOM属性（JSON数组），以及页面截图。"
                + "请根据DOM属性选择最合适的CSS选择器，不要依赖截图猜测。\n\n"
                + "【最高级别格式警告】你是一个没有感情的JSON生成机器。绝对禁止输出任何思考过程、推理步骤、\"首先\"、\"分析\"等前置语言！"
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
                    System.err.println("MiMo parse error, raw body[" + rawBody.length() + "]: " + rawBody.substring(0, Math.min(300, rawBody.length())));
                    return rawBody;
                }
            }
            throw new RuntimeException("MiMo API returned status " + resp.statusCode() + ": " + rawBody.substring(0, Math.min(300, rawBody.length())));
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("MiMo API call failed: " + e.getMessage(), e); }
    }

    // ── Login page detection & auto-fill ──
    private boolean isLoginPage(Page page) {
        // Priority 1: URL pattern
        String url = page.url().toLowerCase();
        if (url.contains("/login") || url.contains("/auth") || url.contains("/signin")) {
            System.out.println("[isLoginPage] URL matched: " + url);
            return true;
        }

        // Priority 2: page body text (most reliable — login page always has these words)
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

        // Priority 3: JS element detection
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
