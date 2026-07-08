package org.example.testvue.controller;

import org.example.testvue.entity.*;
import org.example.testvue.repository.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation")
public class UiAutomationMockController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UiTestScriptRepository scriptRepo;
    private final UiPageObjectRepository pageObjRepo;
    private final UiTestEnvironmentRepository testEnvRepo;
    private final UiOperationRecordRepository opRecordRepo;
    private final UiScheduledTaskRepository scheduledTaskRepo;
    private final UiProjectRepository projectRepo;
    private final UiTestCaseRepository caseRepo;
    private final UiTestSuiteRepository suiteRepo;
    private final UiTestExecutionRepository execRepo;
    private final UiElementRepository elementRepo;
    private final UiNotificationConfigRepository notifConfigRepo;
    private final UiNotificationLogRepository notifLogRepo;

    public UiAutomationMockController(UiTestScriptRepository scriptRepo, UiPageObjectRepository pageObjRepo,
                                      UiTestEnvironmentRepository testEnvRepo, UiOperationRecordRepository opRecordRepo,
                                      UiScheduledTaskRepository scheduledTaskRepo, UiProjectRepository projectRepo,
                                      UiTestCaseRepository caseRepo, UiTestSuiteRepository suiteRepo,
                                      UiTestExecutionRepository execRepo, UiElementRepository elementRepo,
                                      UiNotificationConfigRepository notifConfigRepo, UiNotificationLogRepository notifLogRepo) {
        this.scriptRepo = scriptRepo;
        this.pageObjRepo = pageObjRepo;
        this.testEnvRepo = testEnvRepo;
        this.opRecordRepo = opRecordRepo;
        this.scheduledTaskRepo = scheduledTaskRepo;
        this.projectRepo = projectRepo;
        this.caseRepo = caseRepo;
        this.suiteRepo = suiteRepo;
        this.execRepo = execRepo;
        this.elementRepo = elementRepo;
        this.notifConfigRepo = notifConfigRepo;
        this.notifLogRepo = notifLogRepo;
    }

    // ── Dashboard ──
    @GetMapping("/dashboard/stats/")
    public Map<String, Object> dashboardStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("project_count", projectRepo.count());
        data.put("test_case_count", caseRepo.count());
        data.put("test_suite_count", suiteRepo.count());
        data.put("execution_count", execRepo.count());
        data.put("element_count", elementRepo.count());
        data.put("script_count", scriptRepo.count());
        return data;
    }

    // ── Locator Strategies (static reference data) ──
    @GetMapping("/locator-strategies/")
    public List<Map<String, Object>> locatorStrategies() {
        return Arrays.asList(
            Map.of("id", 1, "name", "CSS Selector"),
            Map.of("id", 2, "name", "XPath"),
            Map.of("id", 3, "name", "ID"),
            Map.of("id", 4, "name", "Name"),
            Map.of("id", 5, "name", "Class Name"),
            Map.of("id", 6, "name", "Tag Name"),
            Map.of("id", 7, "name", "Link Text"),
            Map.of("id", 8, "name", "Partial Link Text"),
            Map.of("id", 9, "name", "Text Content"),
            Map.of("id", 10, "name", "Placeholder"),
            Map.of("id", 11, "name", "Test ID (data-testid)"),
            Map.of("id", 12, "name", "Role (ARIA)"),
            Map.of("id", 13, "name", "Alt Text"),
            Map.of("id", 14, "name", "Title Attribute"),
            Map.of("id", 15, "name", "Custom Attribute")
        );
    }

    // ═══════════════ Test Scripts (JPA) ═══════════════
    @GetMapping("/test-scripts/")
    public Map<String, Object> listScripts(@RequestParam(required = false) Long project) {
        List<UiTestScript> all = (project != null) ? scriptRepo.findByProjectId(project) : scriptRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiTestScript s : all) results.add(scriptToMap(s));
        return paginated(results);
    }

    @PostMapping("/test-scripts/")
    public Map<String, Object> createScript(@RequestBody Map<String, Object> body) {
        UiTestScript s = new UiTestScript();
        s.setName((String) body.getOrDefault("name", "New Script"));
        s.setDescription((String) body.getOrDefault("description", ""));
        s.setLanguage((String) body.getOrDefault("language", "python"));
        s.setEngine((String) body.getOrDefault("engine", "playwright"));
        s.setContent((String) body.getOrDefault("content", ""));
        s.setProjectId(toLong(body.get("project")));
        s = scriptRepo.save(s);
        logOp("create", "创建脚本: " + s.getName());
        Map<String, Object> r = scriptToMap(s); r.put("message", "success"); return r;
    }

    @PatchMapping("/test-scripts/{id}/")
    public Map<String, Object> updateScript(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return scriptRepo.findById(id).map(s -> {
            if (body.containsKey("name")) s.setName((String) body.get("name"));
            if (body.containsKey("content")) s.setContent((String) body.get("content"));
            s.setUpdatedAt(LocalDateTime.now());
            scriptRepo.save(s);
            Map<String, Object> r = scriptToMap(s); r.put("message", "success"); return r;
        }).orElse(notFound());
    }

    @DeleteMapping("/test-scripts/{id}/")
    public Map<String, Object> deleteScript(@PathVariable Long id) {
        scriptRepo.deleteById(id);
        return Map.of("message", "success");
    }

    // ═══════════════ Page Objects (JPA) ═══════════════
    @GetMapping("/page-objects/")
    public Map<String, Object> listPageObjects(@RequestParam(required = false) Long project) {
        List<UiPageObject> all = (project != null) ? pageObjRepo.findByProjectId(project) : pageObjRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiPageObject p : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId()); m.put("name", p.getName()); m.put("description", p.getDescription());
            m.put("url", p.getUrl()); m.put("project_id", p.getProjectId());
            m.put("created_at", fmt(p.getCreatedAt())); m.put("updated_at", fmt(p.getUpdatedAt()));
            results.add(m);
        }
        return paginated(results);
    }

    @PostMapping("/page-objects/")
    public Map<String, Object> createPageObject(@RequestBody Map<String, Object> body) {
        UiPageObject p = new UiPageObject();
        p.setName((String) body.getOrDefault("name", "New Page"));
        p.setDescription((String) body.getOrDefault("description", ""));
        p.setUrl((String) body.getOrDefault("url", ""));
        p.setProjectId(toLong(body.get("project_id")));
        p = pageObjRepo.save(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", p.getId()); r.put("name", p.getName()); r.put("message", "success"); return r;
    }

    @PatchMapping("/page-objects/{id}/")
    public Map<String, Object> updatePageObject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var opt = pageObjRepo.findById(id);
        if (opt.isEmpty()) return notFound();
        var p = opt.get();
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("url")) p.setUrl((String) body.get("url"));
        p.setUpdatedAt(LocalDateTime.now());
        pageObjRepo.save(p);
        return okMap(p.getId(), p.getName());
    }

    @DeleteMapping("/page-objects/{id}/")
    public Map<String, Object> deletePageObject(@PathVariable Long id) {
        pageObjRepo.deleteById(id);
        return Map.of("message", "success");
    }

    // ═══════════════ Test Environments (JPA) ═══════════════
    @GetMapping("/test-environments/")
    public List<Map<String, Object>> listTestEnvironments(@RequestParam(required = false) Long project) {
        List<UiTestEnvironment> all = (project != null) ? testEnvRepo.findByProjectId(project) : testEnvRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiTestEnvironment e : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId()); m.put("name", e.getName()); m.put("description", e.getDescription());
            m.put("browser", e.getBrowser()); m.put("base_url", e.getBaseUrl());
            m.put("variables", e.getVariables()); m.put("project_id", e.getProjectId());
            m.put("is_active", e.getIsActive());
            results.add(m);
        }
        return results;
    }

    @PostMapping("/test-environments/")
    public Map<String, Object> createTestEnvironment(@RequestBody Map<String, Object> body) {
        UiTestEnvironment e = new UiTestEnvironment();
        e.setName((String) body.getOrDefault("name", "New Env"));
        e.setBrowser((String) body.getOrDefault("browser", "chromium"));
        e.setBaseUrl((String) body.getOrDefault("base_url", ""));
        e.setProjectId(toLong(body.get("project_id")));
        e = testEnvRepo.save(e);
        return Map.of("id", e.getId(), "name", e.getName(), "message", "success");
    }

    @PatchMapping("/test-environments/{id}/")
    public Map<String, Object> updateTestEnvironment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return testEnvRepo.findById(id).map(e -> {
            if (body.containsKey("name")) e.setName((String) body.get("name"));
            if (body.containsKey("base_url")) e.setBaseUrl((String) body.get("base_url"));
            e.setUpdatedAt(LocalDateTime.now());
            testEnvRepo.save(e);
            Map<String, Object> r = new LinkedHashMap<>(); r.put("id", e.getId()); r.put("message", "success"); return r;
        }).orElse(notFound());
    }

    @DeleteMapping("/test-environments/{id}/")
    public Map<String, Object> deleteTestEnvironment(@PathVariable Long id) {
        testEnvRepo.deleteById(id);
        return Map.of("message", "success");
    }

    // ═══════════════ Operation Records (JPA) ═══════════════
    @GetMapping("/operation-records/")
    public Map<String, Object> listOperationRecords() {
        List<UiOperationRecord> all = opRecordRepo.findTop20ByOrderByCreatedAtDesc();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiOperationRecord r : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId()); m.put("operation_type", r.getOperationType());
            m.put("description", r.getDescription()); m.put("user_name", r.getUserName());
            m.put("created_at", fmt(r.getCreatedAt()));
            results.add(m);
        }
        return paginated(results);
    }

    // ═══════════════ Scheduled Tasks (JPA) ═══════════════
    @GetMapping("/scheduled-tasks/")
    public Map<String, Object> listScheduledTasks() {
        List<UiScheduledTask> all = scheduledTaskRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiScheduledTask t : all) results.add(taskToMap(t));
        return paginated(results);
    }

    @PostMapping("/scheduled-tasks/")
    public Map<String, Object> createScheduledTask(@RequestBody Map<String, Object> body) {
        UiScheduledTask t = new UiScheduledTask();
        t.setName((String) body.getOrDefault("name", "New Task"));
        t.setDescription((String) body.getOrDefault("description", ""));
        t.setTaskType((String) body.getOrDefault("task_type", "TEST_SUITE"));
        t.setTriggerType((String) body.getOrDefault("trigger_type", "CRON"));
        t.setCronExpression((String) body.getOrDefault("cron_expression", "0 8 * * *"));
        t.setEngine((String) body.getOrDefault("engine", "playwright"));
        t = scheduledTaskRepo.save(t);
        Map<String, Object> r = taskToMap(t); r.put("message", "success"); return r;
    }

    @PatchMapping("/scheduled-tasks/{id}/")
    public Map<String, Object> updateScheduledTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return scheduledTaskRepo.findById(id).map(t -> {
            if (body.containsKey("name")) t.setName((String) body.get("name"));
            if (body.containsKey("cron_expression")) t.setCronExpression((String) body.get("cron_expression"));
            if (body.containsKey("status")) t.setStatus((String) body.get("status"));
            t.setUpdatedAt(LocalDateTime.now());
            scheduledTaskRepo.save(t);
            Map<String, Object> r = taskToMap(t); r.put("message", "success"); return r;
        }).orElse(notFound());
    }

    @DeleteMapping("/scheduled-tasks/{id}/")
    public Map<String, Object> deleteScheduledTask(@PathVariable Long id) {
        scheduledTaskRepo.deleteById(id);
        return Map.of("message", "success");
    }

    @PostMapping("/scheduled-tasks/{id}/activate/")
    public Map<String, Object> activateTask(@PathVariable Long id) {
        scheduledTaskRepo.findById(id).ifPresent(t -> { t.setStatus("active"); scheduledTaskRepo.save(t); });
        return Map.of("message", "已激活");
    }

    @PostMapping("/scheduled-tasks/{id}/pause/")
    public Map<String, Object> pauseTask(@PathVariable Long id) {
        scheduledTaskRepo.findById(id).ifPresent(t -> { t.setStatus("paused"); scheduledTaskRepo.save(t); });
        return Map.of("message", "已暂停");
    }

    @PostMapping("/scheduled-tasks/{id}/run_now/")
    public Map<String, Object> runTaskNow(@PathVariable Long id) {
        scheduledTaskRepo.findById(id).ifPresent(t -> { t.setLastRunAt(LocalDateTime.now()); scheduledTaskRepo.save(t); });
        return Map.of("message", "已触发执行");
    }

    // ═══════════════ Notification Configs (JPA) ═══════════════
    @GetMapping("/notification-configs/")
    public Map<String, Object> listNotificationConfigs() {
        List<UiNotificationConfig> all = notifConfigRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiNotificationConfig c : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("name", c.getName()); m.put("type", c.getType());
            m.put("config", c.getConfig()); m.put("enabled", c.getEnabled());
            results.add(m);
        }
        return paginated(results);
    }

    @PostMapping("/notification-configs/")
    public Map<String, Object> createNotificationConfig(@RequestBody Map<String, Object> body) {
        UiNotificationConfig c = new UiNotificationConfig();
        c.setName((String) body.getOrDefault("name", "New Config"));
        c.setType((String) body.getOrDefault("type", "email"));
        c.setConfig((String) body.getOrDefault("config", "{}"));
        c = notifConfigRepo.save(c);
        logOp("create", "创建通知配置: " + c.getName());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", c.getId()); r.put("name", c.getName()); r.put("message", "success"); return r;
    }

    @PatchMapping("/notification-configs/{id}/")
    public Map<String, Object> updateNotificationConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return notifConfigRepo.findById(id).map(c -> {
            if (body.containsKey("name")) c.setName((String) body.get("name"));
            if (body.containsKey("enabled")) c.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
            c.setUpdatedAt(LocalDateTime.now());
            notifConfigRepo.save(c);
            logOp("edit", "编辑通知配置: " + c.getName());
            return okMap(c.getId(), c.getName());
        }).orElse(notFound());
    }

    @DeleteMapping("/notification-configs/{id}/")
    public Map<String, Object> deleteNotificationConfig(@PathVariable Long id) {
        notifConfigRepo.findById(id).ifPresent(c -> logOp("delete", "删除通知配置: " + c.getName()));
        notifConfigRepo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    // ═══════════════ Notification Logs (JPA) ═══════════════
    @GetMapping("/notification-logs/")
    public Map<String, Object> listNotificationLogs() {
        List<UiNotificationLog> all = notifLogRepo.findTop20ByOrderByCreatedAtDesc();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiNotificationLog l : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId()); m.put("type", l.getType()); m.put("status", l.getStatus());
            m.put("message", l.getMessage()); m.put("recipient", l.getRecipient());
            m.put("created_at", fmt(l.getCreatedAt()));
            results.add(m);
        }
        return paginated(results);
    }

    @GetMapping("/script-steps/")
    public Map<String, Object> listScriptSteps() { return paginated(List.of()); }

    @GetMapping("/screenshots/")
    public List<Map<String, Object>> listScreenshots() { return List.of(); }

    @PostMapping("/screenshots/")
    public Map<String, Object> uploadScreenshot(@RequestBody Map<String, Object> body) {
        return Map.of("id", System.currentTimeMillis(), "message", "success");
    }

    @GetMapping("/script-element-usages/")
    public List<Map<String, Object>> listScriptElementUsages() { return List.of(); }

    // ═══════════════ helpers ═══════════════

    private Map<String, Object> scriptToMap(UiTestScript s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId()); m.put("name", s.getName());
        m.put("description", s.getDescription()); m.put("language", s.getLanguage());
        m.put("engine", s.getEngine()); m.put("content", s.getContent());
        m.put("project_id", s.getProjectId()); m.put("project", Map.of("id", s.getProjectId() != null ? s.getProjectId() : 0, "name", ""));
        m.put("created_at", fmt(s.getCreatedAt())); m.put("updated_at", fmt(s.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> taskToMap(UiScheduledTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId()); m.put("name", t.getName()); m.put("description", t.getDescription());
        m.put("task_type", t.getTaskType()); m.put("trigger_type", t.getTriggerType());
        m.put("cron_expression", t.getCronExpression()); m.put("engine", t.getEngine());
        m.put("status", t.getStatus());
        m.put("created_at", fmt(t.getCreatedAt())); m.put("updated_at", fmt(t.getUpdatedAt()));
        return m;
    }

    private Map<String, Object> paginated(List<Map<String, Object>> results) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size()); resp.put("next", null); resp.put("previous", null);
        resp.put("results", results);
        return resp;
    }

    private String fmt(LocalDateTime t) { return t != null ? t.format(DT) : null; }

    private Map<String, Object> okMap(Long id, String name) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id); r.put("name", name); r.put("message", "success");
        return r;
    }

    private void logOp(String type, String desc) {
        UiOperationRecord r = new UiOperationRecord();
        r.setOperationType(type); r.setDescription(desc); r.setUserName("admin");
        opRecordRepo.save(r);
    }

    private Map<String, Object> notFound() { return new LinkedHashMap<>(Map.of("error", "not found")); }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.valueOf(v.toString()); } catch (NumberFormatException e) { return null; }
    }
}
