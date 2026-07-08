package org.example.testvue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.testvue.entity.*;
import org.example.testvue.repository.*;
import org.example.testvue.service.PlaywrightTestRunner;
import org.example.testvue.service.WebSocketSessionManager;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation")
public class UiTestCaseController {

    private final UiTestCaseRepository repo;
    private final UiTestExecutionRepository execRepo;
    private final UiProjectRepository projectRepo;
    private final UiElementRepository elementRepo;
    private final WebSocketSessionManager wsManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public UiTestCaseController(UiTestCaseRepository repo, UiTestExecutionRepository execRepo,
                                UiProjectRepository projectRepo, UiElementRepository elementRepo,
                                WebSocketSessionManager wsManager) {
        this.repo = repo; this.execRepo = execRepo;
        this.projectRepo = projectRepo; this.elementRepo = elementRepo;
        this.wsManager = wsManager;
    }

    @GetMapping("/test-cases/")
    public Map<String, Object> list(@RequestParam(required = false) Long project,
                                     @RequestParam(required = false) String status) {
        List<UiTestCase> all = (project != null) ? repo.findByProjectId(project) : repo.findAll();
        if (status != null && !status.isBlank()) {
            all = all.stream().filter(t -> status.equals(t.getStatus())).toList();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiTestCase t : all) results.add(toMap(t));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size()); resp.put("results", results);
        return resp;
    }

    @GetMapping("/test-cases/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/test-cases/")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        UiTestCase t = new UiTestCase();
        updateFromBody(t, body);
        t.setCreatedAt(LocalDateTime.now()); t.setUpdatedAt(LocalDateTime.now());
        t = repo.save(t);
        return toMap(t);
    }

    @PatchMapping("/test-cases/{id}/")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(t -> {
            updateFromBody(t, body);
            t.setUpdatedAt(LocalDateTime.now());
            repo.save(t);
            return toMap(t);
        }).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/test-cases/{id}/copy_case/")
    public Map<String, Object> copy(@PathVariable Long id) {
        UiTestCase src = repo.findById(id).orElse(null);
        if (src == null) return Map.of("error", "not found");
        UiTestCase cp = new UiTestCase();
        cp.setName(src.getName() + " - 副本");
        cp.setDescription(src.getDescription());
        cp.setPriority(src.getPriority());
        cp.setSteps(src.getSteps());
        cp.setProjectId(src.getProjectId());
        cp.setEngine(src.getEngine());
        cp.setBrowser(src.getBrowser());
        cp.setCreatedAt(LocalDateTime.now());
        cp.setUpdatedAt(LocalDateTime.now());
        cp = repo.save(cp);
        return toMap(cp);
    }

    @DeleteMapping("/test-cases/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    @PostMapping("/test-cases/{id}/run/")
    public Map<String, Object> run(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        UiTestCase tc = repo.findById(id).orElse(null);
        if (tc == null) return Map.of("success", false, "error", "Test case not found");

        // Lookup project for baseUrl
        String baseUrl = "";
        if (tc.getProjectId() != null) {
            baseUrl = projectRepo.findById(tc.getProjectId()).map(UiProject::getBaseUrl).orElse("");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return Map.of("success", false, "error", "请先在项目管理中配置项目的 baseUrl");
        }

        // Build element lookup map (element_id → locator info)
        Map<Long, Map<String, Object>> elements = new LinkedHashMap<>();
        List<UiElement> elList = tc.getProjectId() != null ? elementRepo.findByProjectId(tc.getProjectId()) : List.of();
        if (tc.getProjectId() != null) {
            for (UiElement el : elList) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("locator_value", el.getLocatorValue());
                info.put("locator_strategy", el.getLocatorStrategy());
                info.put("element_type", el.getElementType());
                info.put("wait_timeout", el.getWaitTimeout());
                info.put("force_action", el.getForceAction());
                elements.put(el.getId(), info);
            }
        }

        // Engine / browser / headless from body or fallback to test case settings
        String engine = body != null && body.containsKey("engine") ? (String) body.get("engine") : tc.getEngine();
        String browser = body != null && body.containsKey("browser") ? (String) body.get("browser") : tc.getBrowser();
        boolean headless = body != null && !Boolean.FALSE.equals(body.get("headless"));

        // Variable resolver: resolve {{function(args)}} patterns
        java.util.function.Function<String, String> variableResolver = expr -> {
            try {
                String name = expr.contains("(") ? expr.substring(0, expr.indexOf("(")).trim() : expr.trim();
                return switch (name) {
                    case "random_int" -> String.valueOf((int)(Math.random() * 100));
                    case "random_float" -> String.format("%.2f", Math.random() * 100);
                    case "random_string" -> java.util.UUID.randomUUID().toString().substring(0, 8);
                    case "random_uuid" -> java.util.UUID.randomUUID().toString();
                    case "random_date" -> "2026-07-0" + (1 + (int)(Math.random() * 9));
                    case "generate_chinese_name" -> new String[]{"张伟","王芳","李娜","刘洋","陈静"}[(int)(Math.random() * 5)];
                    case "generate_chinese_phone" -> new String[]{"13800138001","13912345678"}[(int)(Math.random() * 2)];
                    case "generate_chinese_email" -> "test" + (int)(Math.random()*1000) + "@example.com";
                    case "generate_id_card" -> "11010119900101" + String.format("%04d", (int)(Math.random()*9000)+1000);
                    case "random_password" -> "Pwd@" + java.util.UUID.randomUUID().toString().substring(0, 8);
                    case "timestamp" -> String.valueOf(System.currentTimeMillis());
                    case "base64_encode" -> java.util.Base64.getEncoder().encodeToString(expr.substring(expr.indexOf("(") + 1, expr.lastIndexOf(")")).replaceAll("['\"]", "").getBytes());
                    case "md5_hash" -> {
                        String inp = expr.substring(expr.indexOf("(") + 1, expr.lastIndexOf(")")).replaceAll("['\"]", "").trim();
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                        byte[] digest = md.digest(inp.getBytes());
                        StringBuilder sb = new StringBuilder();
                        for (byte b : digest) sb.append(String.format("%02x", b));
                        yield sb.toString();
                    }
                    default -> {
                        // Try to call data factory mock endpoint
                        yield "{{" + expr + "}}"; // fallback: leave unresolved
                    }
                };
            } catch (Exception e) {
                return "{{" + expr + "}}";
            }
        };

        // Auto-login: find login test case and prepend its steps
        boolean autoLogin = body != null && Boolean.TRUE.equals(body.get("auto_login"));
        String stepsJson = tc.getSteps();
        if (autoLogin) {
            UiTestCase loginCase = repo.findAll().stream()
                .filter(c -> c.getProjectId() != null && c.getProjectId().equals(tc.getProjectId()))
                .filter(c -> c.getName() != null && c.getName().contains("登录"))
                .filter(c -> c.getSteps() != null && !c.getSteps().isBlank() && !"[]".equals(c.getSteps()))
                .findFirst().orElse(null);
            if (loginCase != null) {
                String loginSteps = loginCase.getSteps();
                if (stepsJson != null && !stepsJson.isBlank() && !"[]".equals(stepsJson)) {
                    stepsJson = loginSteps.substring(0, loginSteps.length() - 1) + "," + stepsJson.substring(1);
                } else {
                    stepsJson = loginSteps;
                }
            }
        }

        // Run with Playwright, push logs via WebSocket in real time
        String taskId = UUID.randomUUID().toString();
        StringBuilder logBuf = new StringBuilder();
        wsManager.pushLine(taskId, "[START] 开始执行...");

        java.util.function.Consumer<String> liveLog = line -> wsManager.pushLine(taskId, line);
        Map<String, Object> runResult = PlaywrightTestRunner.execute(
            baseUrl, stepsJson, elements, variableResolver, engine, browser, headless, logBuf, liveLog
        );
        wsManager.pushLine(taskId, "[DONE] 执行完成");

        // Persist execution record
        UiTestExecution exec = new UiTestExecution();
        exec.setTestCaseId(tc.getId());
        exec.setTestCaseName(tc.getName());
        exec.setProjectId(tc.getProjectId());
        exec.setEngine(engine);
        exec.setBrowser(browser);
        exec.setStatus((String) runResult.get("status"));
        exec.setTotal((Integer) runResult.get("total"));
        exec.setPassed((Integer) runResult.get("passed"));
        exec.setFailed((Integer) runResult.get("failed"));
        exec.setDuration((String) runResult.get("duration"));
        exec.setLogs(logBuf.toString());
        // Convert file paths to [{url: "/screenshots/xxx.png"}, ...] for frontend
        String rawShots = (String) runResult.get("screenshots");
        exec.setScreenshots(rawShots);
        List<Map<String, String>> shotsForUi = new ArrayList<>();
        if (rawShots != null && !rawShots.equals("[]")) {
            try {
                List<String> paths = mapper.readValue(rawShots, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                for (String p : paths) shotsForUi.add(Map.of("url", p));
            } catch (Exception ignored) {}
        }
        exec.setExecutedAt(LocalDateTime.now());
        exec.setCreatedAt(LocalDateTime.now());
        exec = execRepo.save(exec);

        return Map.of(
            "success", true,
            "execution_id", exec.getId(),
            "task_id", taskId,
            "status", exec.getStatus(),
            "data", Map.of(
                "id", exec.getId(),
                "status", exec.getStatus(),
                "passed", exec.getPassed(),
                "failed", exec.getFailed(),
                "total", exec.getTotal(),
                "duration", exec.getDuration(),
                "logs", exec.getLogs(),
                "screenshots", shotsForUi,
                "executed_at", exec.getExecutedAt().toString()
            )
        );
    }

    private Map<String, Object> toMap(UiTestCase t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId()); m.put("name", t.getName());
        m.put("description", t.getDescription()); m.put("priority", t.getPriority());
        m.put("status", t.getStatus()); m.put("steps", t.getSteps());
        m.put("project_id", t.getProjectId()); m.put("engine", t.getEngine());
        m.put("browser", t.getBrowser());
        m.put("created_at", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
        m.put("updated_at", t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        return m;
    }

    private void updateFromBody(UiTestCase t, Map<String, Object> body) {
        if (body.containsKey("name")) t.setName((String) body.get("name"));
        if (body.containsKey("description")) t.setDescription((String) body.get("description"));
        if (body.containsKey("priority")) t.setPriority((String) body.get("priority"));
        if (body.containsKey("status")) t.setStatus((String) body.get("status"));
        if (body.containsKey("steps") && body.get("steps") != null) {
            try { t.setSteps(mapper.writeValueAsString(body.get("steps"))); } catch (Exception e) { t.setSteps("[]"); }
        }
        if (body.containsKey("project_id") && body.get("project_id") != null)
            t.setProjectId(Long.valueOf(body.get("project_id").toString()));
        if (body.containsKey("engine")) t.setEngine((String) body.get("engine"));
        if (body.containsKey("browser")) t.setBrowser((String) body.get("browser"));
    }
}
