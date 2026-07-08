package org.example.testvue.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlaywrightTestRunner {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SCREENSHOT_DIR = "target/screenshots";

    private final StringBuilder log;
    private final List<String> screenshotPaths;
    private final Map<Long, Map<String, Object>> elements;
    private final String baseUrl;
    private final java.util.function.Function<String, String> variableResolver;
    private final String runId;
    private final java.util.function.Consumer<String> liveLog; // real-time log callback

    private int passed, failed;

    public PlaywrightTestRunner(String baseUrl, Map<Long, Map<String, Object>> elements,
                                 java.util.function.Function<String, String> variableResolver,
                                 StringBuilder logBuf) {
        this(baseUrl, elements, variableResolver, logBuf, null);
    }

    public PlaywrightTestRunner(String baseUrl, Map<Long, Map<String, Object>> elements,
                                 java.util.function.Function<String, String> variableResolver,
                                 StringBuilder logBuf, java.util.function.Consumer<String> liveLog) {
        this.baseUrl = baseUrl;
        this.elements = elements;
        this.variableResolver = variableResolver;
        this.log = logBuf;
        this.liveLog = liveLog;
        this.runId = UUID.randomUUID().toString().substring(0, 8);
        this.screenshotPaths = new ArrayList<>();
        new File(SCREENSHOT_DIR).mkdirs();
    }

    public Map<String, Object> run(String stepsJson, String engine, String browser, boolean headless) {
        long startMs = System.currentTimeMillis();
        passed = 0;
        failed = 0;

        List<Map<String, Object>> steps = parseSteps(stepsJson);
        int total = steps.size();

        log("=== TaaS Playwright Runner ===");
        log("Browser: " + browser + " | Headless: " + headless);
        log("Base URL: " + baseUrl);
        log("Steps: " + total);

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions().setHeadless(headless);
            Browser br = switch (browser != null ? browser.toLowerCase() : "chromium") {
                case "firefox" -> playwright.firefox().launch(opts);
                case "webkit" -> playwright.webkit().launch(opts);
                default -> playwright.chromium().launch(opts);
            };

            BrowserContext ctx = br.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
            Page page = ctx.newPage();

            // Auto-navigate to baseUrl if first step is not a navigate action
            if (!steps.isEmpty()) {
                String firstAction = (String) steps.get(0).getOrDefault("action_type", "");
                if (!"navigate".equals(firstAction) && baseUrl != null && !baseUrl.isBlank()) {
                    log("[AUTO] Navigating to base URL: " + baseUrl);
                    page.navigate(baseUrl);
                    page.waitForLoadState();
                    log("  ✓ Page loaded");
                }
            }

            for (int i = 0; i < steps.size(); i++) {
                Map<String, Object> step = steps.get(i);
                // Resolve {{variables}} in input_value before execution
                Object iv = step.get("input_value");
                if (iv instanceof String) {
                    step.put("input_value", resolveVariables((String) iv));
                }
                String action = (String) step.getOrDefault("action_type", "");
                Object waitTimeObj = step.get("wait_time");
                int waitTime = waitTimeObj instanceof Number ? ((Number) waitTimeObj).intValue() : 1000;

                log(String.format("[%d/%d] %s", i + 1, total, action.toUpperCase()));

                try {
                    switch (action) {
                        case "navigate" -> runNavigate(step, page);
                        case "click" -> {
                            String ct = (String) step.getOrDefault("click_type", "single");
                            Locator cl = resolveLocator(page, step.get("element_id"));
                            if ("double".equals(ct)) { log("  → Double click"); cl.dblclick(); passed++; }
                            else if ("right".equals(ct)) { log("  → Right click"); cl.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT)); passed++; }
                            else { log("  → " + cl); cl.click(); passed++; }
                        }
                        case "fill" -> runFill(step, page);
                        case "getText" -> runGetText(step, page);
                        case "waitFor" -> runWaitFor(step, page, waitTime);
                        case "hover" -> runHover(step, page);
                        case "scroll" -> runScroll(step, page);
                        case "screenshot" -> runScreenshot(page);
                        case "assert" -> runAssert(step, page);
                        case "apiAssert" -> runApiAssert(step, ctx);
                        case "wait" -> {
                            page.waitForTimeout(waitTime);
                            log("  ✓ Waited " + waitTime + "ms");
                            passed++;
                        }
                        case "switchTab" -> {
                            page = switchToTab(ctx, step);
                            log("  ✓ Switched tab");
                            passed++;
                        }
                        case "press" -> {
                            String key = (String) step.getOrDefault("input_value", "Enter");
                            log("  → Press: " + key);
                            page.keyboard().press(key);
                            passed++;
                        }
                        default -> {
                            log("  - Unknown action: " + action + " (skipped)");
                            failed++;
                        }
                    }
                } catch (Exception e) {
                    log("  ✗ ERROR: " + e.getMessage());
                    failed++;
                    try {
                        String path = saveShot(page);
                        screenshotPaths.add(path);
                        log("  Screenshot saved: " + path);
                    } catch (Exception se) {
                        log("  (could not capture screenshot)");
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            log("FATAL: " + e.getMessage());
            if (failed == 0) failed = 1;
        }

        long durationMs = System.currentTimeMillis() - startMs;
        String durationStr = durationMs >= 60000
            ? (durationMs / 60000) + "m" + ((durationMs % 60000) / 1000) + "s"
            : String.format("%.1fs", durationMs / 1000.0);

        String finalStatus = total == 0 ? "skipped" : (failed == 0 ? "passed" : "failed");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", finalStatus);
        result.put("passed", passed);
        result.put("failed", failed);
        result.put("total", total);
        result.put("duration", durationStr);
        result.put("logs", log.toString());
        // Keep max 5 screenshot paths
        List<String> trimmed = screenshotPaths.size() > 5 ? screenshotPaths.subList(screenshotPaths.size() - 5, screenshotPaths.size()) : screenshotPaths;
        result.put("screenshots", mapper.valueToTree(trimmed).toString());
        return result;
    }

    // ── step runners ──

    private void runNavigate(Map<String, Object> step, Page page) {
        String input = (String) step.getOrDefault("input_value", "");
        String url = toNavUrl(input);
        log("  → " + url);
        page.navigate(url);
        page.waitForLoadState();
        log("  ✓ Loaded");
        passed++;
    }

    private void runClick(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        log("  → " + loc);
        loc.click();
        passed++;
    }

    private void runDoubleClick(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        log("  → Double click: " + loc);
        loc.dblclick();
        passed++;
    }

    private void runRightClick(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        log("  → Right click: " + loc);
        loc.click(new Locator.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT));
        passed++;
    }

    private void runFill(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        String val = (String) step.getOrDefault("input_value", "");
        log("  → Fill: " + val);
        loc.fill(val != null ? val : "");
        String pressAfter = (String) step.getOrDefault("press_after", "");
        if (!pressAfter.isBlank()) {
            page.keyboard().press(pressAfter);
            log("  → Pressed: " + pressAfter);
        }
        passed++;
    }

    private void runGetText(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        String text = loc.textContent();
        log("  → Text: " + (text != null ? text.substring(0, Math.min(300, text.length())) : "(null)"));
        passed++;
    }

    private void runWaitFor(Map<String, Object> step, Page page, int timeout) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        log("  → Waiting " + timeout + "ms");
        loc.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        log("  ✓ Found");
        passed++;
    }

    private void runHover(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        loc.hover();
        log("  ✓ Hovered");
        passed++;
    }

    private void runScroll(Map<String, Object> step, Page page) {
        Locator loc = resolveLocator(page, step.get("element_id"));
        loc.scrollIntoViewIfNeeded();
        log("  ✓ Scrolled");
        passed++;
    }

    private void runScreenshot(Page page) {
        String path = saveShot(page);
        screenshotPaths.add(path);
        log("  ✓ Screenshot: " + path);
        passed++;
    }

    @SuppressWarnings("unchecked")
    private void runAssert(Map<String, Object> step, Page page) {
        String assertType = (String) step.getOrDefault("assert_type", "textContains");
        String assertValue = (String) step.getOrDefault("assert_value", "");
        // 没选元素时，对整个页面做文本断言
        Object eid = step.get("element_id");
        Locator loc = (eid == null || "".equals(eid.toString()))
            ? page.locator("body")
            : resolveLocator(page, eid);
        boolean ok = switch (assertType) {
            case "textContains" -> {
                String t = loc.textContent();
                if (t == null) yield false;
                if (assertValue != null && (assertValue.contains("*") || assertValue.contains("?"))) {
                    String regex = assertValue.replace("*", ".*").replace("?", ".");
                    yield t.matches(".*" + regex + ".*");
                }
                yield t.contains(assertValue);
            }
            case "textEquals" -> {
                String t = loc.textContent();
                yield t != null && t.equals(assertValue);
            }
            case "isVisible" -> loc.isVisible();
            case "exists" -> loc.count() > 0;
            case "hasAttribute" -> {
                if (assertValue == null || !assertValue.contains("=")) yield false;
                String[] parts = assertValue.split("=", 2);
                String attr = loc.getAttribute(parts[0]);
                yield parts[1].equals(attr);
            }
            default -> loc.isVisible();
        };
        if (ok) {
            log("  ✓ Assert " + assertType + " passed");
            passed++;
        } else {
            log("  ✗ Assert " + assertType + " FAILED (expected: " + assertValue + ")");
            failed++;
            String path = saveShot(page);
            screenshotPaths.add(path);
        }
    }

    private void runApiAssert(Map<String, Object> step, BrowserContext ctx) {
        String url = (String) step.getOrDefault("input_value", "");
        String assertType = (String) step.getOrDefault("assert_type", "textContains");
        String assertValue = (String) step.getOrDefault("assert_value", "");
        if (url.isBlank()) { log("  ✗ API断言: URL为空"); failed++; return; }
        if (!url.startsWith("http") && baseUrl != null && !baseUrl.isBlank()) {
            url = baseUrl.replaceAll("/+$", "") + "/" + url.replaceAll("^/+", "");
        }
        log("  → API: GET " + url);
        try {
            APIResponse resp = ctx.request().get(url);
            String body = resp.text();
            int status = resp.status();
            log("  ← HTTP " + status + " body=" + (body != null ? body.substring(0, Math.min(200, body.length())) : "null"));
            boolean ok = switch (assertType) {
                case "statusEquals" -> status == Integer.parseInt(assertValue);
                case "textContains" -> body != null && body.contains(assertValue);
                case "textEquals" -> body != null && body.equals(assertValue);
                case "jsonPath" -> body != null && body.contains(assertValue);
                default -> status >= 200 && status < 300;
            };
            if (ok) { log("  ✓ API Assert passed"); passed++; }
            else { log("  ✗ API Assert FAILED (expected: " + assertValue + ")"); failed++; }
        } catch (Exception e) {
            log("  ✗ API Error: " + e.getMessage());
            failed++;
        }
    }

    private Page switchToTab(BrowserContext ctx, Map<String, Object> step) {
        List<Page> pages = ctx.pages();
        String input = (String) step.getOrDefault("input_value", "");
        int idx = -1;
        try { idx = Integer.parseInt(input); } catch (Exception ignored) {}

        if (idx >= 0 && idx < pages.size()) {
            pages.get(idx).bringToFront();
            return pages.get(idx);
        }
        // switch to last tab
        if (pages.size() > 0) {
            pages.get(pages.size() - 1).bringToFront();
            return pages.get(pages.size() - 1);
        }
        return ctx.newPage();
    }

    // ── helpers ──

    private List<Map<String, Object>> parseSteps(String stepsJson) {
        try {
            if (stepsJson == null || stepsJson.isBlank()) return List.of();
            return mapper.readValue(stepsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toNavUrl(String input) {
        if (input == null || input.isBlank()) {
            return baseUrl != null ? baseUrl : "about:blank";
        }
        if (input.startsWith("http://") || input.startsWith("https://")) return input;
        if (input.startsWith("/") && baseUrl != null) {
            String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return b + input;
        }
        return baseUrl != null ? baseUrl : input;
    }

    @SuppressWarnings("unchecked")
    private Locator resolveLocator(Page page, Object elemIdObj) {
        if (elemIdObj instanceof Number) {
            Long eid = ((Number) elemIdObj).longValue();
            Map<String, Object> elem = elements.get(eid);
            if (elem != null) {
                Object ls = elem.getOrDefault("locator_strategy", "CSS Selector");
                String strategy = ls instanceof Map ? (String) ((Map<String,Object>) ls).getOrDefault("name", "CSS Selector") : (String) ls;
                String value = (String) elem.getOrDefault("locator_value", "");
                String elemType = (String) elem.getOrDefault("element_type", "BUTTON");
                return buildLocator(page, strategy, value, elemType).first();
            }
        }
        // fallback: raw CSS/XPath from input_value
        return page.locator("#unknown").first();
    }

    private Locator buildLocator(Page page, String strategy, String value, String elemType) {
        if (value == null) value = "";
        // Handle locator("sel").filter(has_text="txt") raw expressions
        String filterText = null;
        if (value.contains(".filter(has_text=")) {
            int filterStart = value.indexOf(".filter(has_text=");
            String sel = value.substring(0, filterStart);
            String f = value.substring(filterStart + ".filter(has_text=".length());
            f = f.replaceAll("[\"'()]", "").trim();
            if (!f.isBlank()) { value = sel; filterText = f; }
        }
        String s = strategy != null ? strategy.toLowerCase() : "";
        // Normalize
        if (s.startsWith("role")) s = "role";
        if (s.equals("css selector")) s = "css";
        if (s.startsWith("text")) s = "text";
        if (s.equals("test id") || s.startsWith("test id")) s = "data-testid";
        if (s.equals("class name")) s = "class";
        if (s.equals("tag name")) s = "tag";
        Locator loc = switch (s) {
            case "xpath" -> page.locator("xpath=" + value);
            case "id" -> page.locator("#" + value.replaceFirst("^#", ""));
            case "name" -> page.locator("[name='" + escapeCss(value) + "']");
            case "class" -> page.locator("." + value.replaceFirst("^\\.", ""));
            case "tag" -> page.locator(value);
            case "link text" -> page.getByText(value, new Page.GetByTextOptions().setExact(true));
            case "partial link text" -> page.getByText(value);
            case "text" -> page.getByText(value);
            case "placeholder" -> page.getByPlaceholder(value);
            case "data-testid" -> page.locator("[data-testid='" + escapeCss(value) + "']");
            case "role" -> page.getByRole(toAriaRole(elemType), new Page.GetByRoleOptions().setName(value));
            case "alt text" -> page.getByAltText(value);
            case "title attribute" -> page.locator("[title='" + escapeCss(value) + "']");
            default -> page.locator(value); // CSS Selector
        };
        if (filterText != null && !filterText.isBlank()) {
            loc = loc.filter(new Locator.FilterOptions().setHasText(filterText));
        }
        return loc;
    }

    /**
     * Map element_type → AriaRole for getByRole() calls.
     */
    private AriaRole toAriaRole(String elementType) {
        if (elementType == null) return AriaRole.BUTTON;
        return switch (elementType.toUpperCase()) {
            case "INPUT", "TEXTAREA" -> AriaRole.TEXTBOX;
            case "BUTTON" -> AriaRole.BUTTON;
            case "CHECKBOX" -> AriaRole.CHECKBOX;
            case "LINK" -> AriaRole.LINK;
            case "IMAGE", "IMG" -> AriaRole.IMG;
            case "DROPDOWN", "SELECT" -> AriaRole.COMBOBOX;
            case "RADIO" -> AriaRole.RADIO;
            case "TABLE" -> AriaRole.TABLE;
            case "FORM" -> AriaRole.FORM;
            case "MODAL", "DIALOG" -> AriaRole.DIALOG;
            case "HEADING" -> AriaRole.HEADING;
            case "LIST" -> AriaRole.LIST;
            case "LISTITEM" -> AriaRole.LISTITEM;
            case "MENU" -> AriaRole.MENU;
            case "MENUITEM" -> AriaRole.MENUITEM;
            case "NAVIGATION" -> AriaRole.NAVIGATION;
            case "TAB" -> AriaRole.TAB;
            case "TABPANEL" -> AriaRole.TABPANEL;
            case "TOOLTIP" -> AriaRole.TOOLTIP;
            case "TREE" -> AriaRole.TREE;
            case "TREEITEM" -> AriaRole.TREEITEM;
            case "PROGRESSBAR" -> AriaRole.PROGRESSBAR;
            case "SLIDER" -> AriaRole.SLIDER;
            case "SEPARATOR" -> AriaRole.SEPARATOR;
            case "ALERT" -> AriaRole.ALERT;
            case "BANNER" -> AriaRole.BANNER;
            case "SEARCHBOX" -> AriaRole.SEARCHBOX;
            case "SWITCH" -> AriaRole.SWITCH;
            case "SPINBUTTON" -> AriaRole.SPINBUTTON;
            case "TEXT" -> AriaRole.TEXTBOX;
            case "CONTAINER" -> AriaRole.GROUP;
            case "CELL" -> AriaRole.CELL;
            case "ROW" -> AriaRole.ROW;
            default -> AriaRole.BUTTON;
        };
    }

    private String saveShot(Page page) {
        try {
            String fname = "ss_" + runId + "_" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(SCREENSHOT_DIR, fname)));
            return "/screenshots/" + fname;
        } catch (Exception e) {
            return "";
        }
    }

    private String escapeCss(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private void log(String msg) {
        log.append(msg).append("\n");
        if (liveLog != null) liveLog.accept(msg);
    }

    // ── variable resolver ──
    private String resolveVariables(String input) {
        if (input == null || !input.contains("{{")) return input;
        StringBuilder sb = new StringBuilder(input);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{(.+?)\\}\\}").matcher(input);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String expr = m.group(1).trim();
            String resolved = variableResolver != null ? variableResolver.apply(expr) : "{{" + expr + "}}";
            m.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(resolved));
        }
        m.appendTail(result);
        return result.toString();
    }

    // ── static entry ──
    public static Map<String, Object> execute(String baseUrl, String stepsJson,
                                               Map<Long, Map<String, Object>> elements,
                                               java.util.function.Function<String, String> variableResolver,
                                               String engine, String browser, boolean headless,
                                               StringBuilder logBuf) {
        return execute(baseUrl, stepsJson, elements, variableResolver, engine, browser, headless, logBuf, null);
    }

    public static Map<String, Object> execute(String baseUrl, String stepsJson,
                                               Map<Long, Map<String, Object>> elements,
                                               java.util.function.Function<String, String> variableResolver,
                                               String engine, String browser, boolean headless,
                                               StringBuilder logBuf, java.util.function.Consumer<String> liveLog) {
        PlaywrightTestRunner runner = new PlaywrightTestRunner(baseUrl, elements, variableResolver, logBuf, liveLog);
        return runner.run(stepsJson, engine, browser, headless);
    }
}
