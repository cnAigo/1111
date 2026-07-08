package org.example.testvue.controller;

import org.example.testvue.entity.AiExecutionRecord;
import org.example.testvue.entity.AiCase;
import org.example.testvue.entity.AiPromptTemplate;
import org.example.testvue.entity.TestCaseStep;
import org.example.testvue.repository.AiExecutionRecordRepository;
import org.example.testvue.repository.AiCaseRepository;
import org.example.testvue.repository.AiPromptTemplateRepository;
import org.example.testvue.repository.TestCaseStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.testvue.service.AutomationEngineService;
import org.example.testvue.service.BrowserAgentService;
import org.example.testvue.service.MiMoService;
import org.example.testvue.service.TestReplayService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ui-automation")
public class AiExecutionController {

    private final AiExecutionRecordRepository repo;
    private final AiCaseRepository caseRepo;
    private final TestCaseStepRepository stepRepo;
    private final AiPromptTemplateRepository promptRepo;
    private final BrowserAgentService browserAgent;
    private final TestReplayService replayService;
    private final AutomationEngineService automationEngine;
    private final MiMoService miMoService;
    private final ObjectMapper mapper = new ObjectMapper();

    // Track running threads so stop() can interrupt them
    private final ConcurrentHashMap<Long, Thread> runningThreads = new ConcurrentHashMap<>();
    private static final long EXECUTION_TIMEOUT_MINUTES = 30;

    public AiExecutionController(AiExecutionRecordRepository repo, AiCaseRepository caseRepo,
                                 TestCaseStepRepository stepRepo, AiPromptTemplateRepository promptRepo,
                                 BrowserAgentService browserAgent, TestReplayService replayService,
                                 AutomationEngineService automationEngine, MiMoService miMoService) {
        this.repo = repo; this.caseRepo = caseRepo;
        this.stepRepo = stepRepo;
        this.promptRepo = promptRepo;
        this.browserAgent = browserAgent;
        this.replayService = replayService;
        this.automationEngine = automationEngine;
        this.miMoService = miMoService;
    }

    // ── AI Execution Records ──
    @GetMapping("/ai-execution-records/")
    public Map<String, Object> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(defaultValue = "") String search) {
        List<AiExecutionRecord> all;
        if (search != null && !search.isEmpty()) {
            all = repo.findAll().stream().filter(r ->
                (r.getTaskDescription() != null && r.getTaskDescription().contains(search)) ||
                (r.getCaseName() != null && r.getCaseName().contains(search))
            ).collect(Collectors.toList());
        } else {
            all = repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        int total = all.size();
        int from = Math.min((page - 1) * page_size, total);
        int to = Math.min(from + page_size, total);
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = from; i < to; i++) results.add(toMap(all.get(i)));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", total);
        resp.put("results", results);
        return resp;
    }

    @GetMapping("/ai-execution-records/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/ai-execution-records/run_adhoc/")
    public Map<String, Object> runAdhoc(@RequestBody Map<String, Object> body) {
        String taskDesc = (String) body.getOrDefault("task_description", "未命名");
        List<Map<String, Object>> plannedTasks = buildPlannedTasks(taskDesc);
        String stepsJson = toJson(plannedTasks);

        AiExecutionRecord r = new AiExecutionRecord();
        r.setTaskDescription(taskDesc);
        r.setCaseName("Ad-hoc任务");
        r.setStatus("running");
        r.setProgress(10);
        r.setResult("开始执行...");
        r.setSteps(stepsJson);
        r.setStartTime(LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r = repo.save(r);
        final Long recId = r.getId();

        Thread t = new Thread(() -> {
            try {
                runAgentAndSave(recId, taskDesc);
            } finally {
                runningThreads.remove(recId);
            }
        });
        runningThreads.put(recId, t);
        t.start();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", r.getId()); resp.put("execution_id", r.getId());
        resp.put("status", "running"); resp.put("message", "AI任务已启动");
        return resp;
    }

    @PostMapping("/ai-execution-records/{id}/stop/")
    public Map<String, Object> stop(@PathVariable Long id) {
        Thread t = runningThreads.remove(id);
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
        repo.findById(id).ifPresent(r -> {
            r.setStatus("stopped");
            if (r.getStartTime() != null) {
                r.setDuration(Duration.between(r.getStartTime(), LocalDateTime.now()).toMillis());
            }
            r.setUpdatedAt(LocalDateTime.now());
            repo.save(r);
        });
        return Map.of("id", id, "message", "已停止");
    }

    @PostMapping("/ai-execution-records/batch_delete/")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        List<Long> ids = new ArrayList<>();
        if (idsObj instanceof List<?> rawList) {
            for (Object o : rawList) {
                if (o instanceof Number n) ids.add(n.longValue());
                else ids.add(Long.parseLong(o.toString()));
            }
        }
        repo.deleteAllById(ids);
        return Map.of("message", "success", "deleted", ids.size());
    }

    @GetMapping("/ai-execution-records/{id}/report/")
    public Map<String, Object> report(@PathVariable Long id) {
        return repo.findById(id).map(rec -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", true);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", rec.getId());
            data.put("task_description", rec.getTaskDescription());
            data.put("status", rec.getStatus());

            // Parse steps JSON
            List<Map<String, Object>> steps = parseSteps(rec.getSteps());
            int totalSteps = steps.size();
            long completed = steps.stream().filter(s -> "completed".equals(s.get("status"))).count();
            long failed = steps.stream().filter(s -> "failed".equals(s.get("status"))).count();
            long pending = steps.stream().filter(s -> "pending".equals(s.get("status"))).count();
            long skipped = steps.stream().filter(s -> "skipped".equals(s.get("status"))).count();

            // Overview
            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("status", rec.getStatus());
            overview.put("total_steps", totalSteps);
            overview.put("completed", completed);
            overview.put("failed", failed);
            overview.put("pending", pending);
            overview.put("skipped", skipped);
            overview.put("duration_formatted", rec.getDuration() != null ? formatDuration(rec.getDuration()) : "N/A");
            double completionRate = totalSteps > 0 ? (completed + failed) * 100.0 / totalSteps : 0;
            overview.put("completion_rate", Math.round(completionRate * 10) / 10.0);
            String statusColor = "completed".equals(rec.getStatus()) ? "#67c23a" :
                "failed".equals(rec.getStatus()) ? "#f56c6c" : "#e6a23c";
            overview.put("status_color", statusColor);
            data.put("overview", overview);

            // Statistics
            Map<String, Object> statistics = new LinkedHashMap<>();
            statistics.put("total", totalSteps);
            statistics.put("completed", completed);
            statistics.put("failed", failed);
            statistics.put("pending", pending);
            statistics.put("skipped", skipped);
            double successRate = (completed + failed) > 0 ? completed * 100.0 / (completed + failed) : 0;
            statistics.put("success_rate", Math.round(successRate * 10) / 10.0);
            data.put("statistics", statistics);

            // Timeline from actual steps
            List<Map<String, Object>> timeline = new ArrayList<>();
            for (Map<String, Object> step : steps) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("id", step.get("id"));
                t.put("description", step.get("description"));
                t.put("status", step.get("status"));
                t.put("status_display", step.get("status"));
                timeline.add(t);
            }
            if (timeline.isEmpty()) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("id", 1);
                t.put("description", "执行完成");
                t.put("status", rec.getStatus());
                t.put("status_display", rec.getStatus());
                timeline.add(t);
            }
            data.put("timeline", timeline);

            // Detailed steps
            List<Map<String, Object>> detailedSteps = new ArrayList<>();
            int stepNum = 0;
            for (Map<String, Object> step : steps) {
                stepNum++;
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("step_number", stepNum);
                ds.put("description", step.getOrDefault("description", ""));
                ds.put("status", step.getOrDefault("status", "pending"));
                ds.put("action", step.getOrDefault("action", ""));
                ds.put("element", step.getOrDefault("target", ""));
                ds.put("thinking", step.getOrDefault("thinking", ""));
                detailedSteps.add(ds);
            }
            data.put("detailed_steps", detailedSteps);

            // Errors from failed steps
            List<Map<String, Object>> errors = new ArrayList<>();
            for (Map<String, Object> step : steps) {
                if ("failed".equals(step.get("status"))) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("step_number", step.get("id"));
                    err.put("type", "step_failure");
                    err.put("message", step.getOrDefault("description", "Execution failed"));
                    err.put("severity", "high");
                    errors.add(err);
                }
            }
            if (errors.isEmpty() && "failed".equals(rec.getStatus())) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("step_number", 0);
                err.put("type", "execution_error");
                err.put("message", rec.getResult() != null ? rec.getResult() : "未知错误");
                err.put("severity", "high");
                errors.add(err);
            }
            data.put("errors", errors);

            // Action distribution as object for frontend chart
            Map<String, Integer> actionCounts = new LinkedHashMap<>();
            for (Map<String, Object> step : steps) {
                String action = (String) step.getOrDefault("action_type", step.getOrDefault("action", ""));
                if (action != null && !action.isEmpty()) {
                    actionCounts.merge(action.toLowerCase(), 1, Integer::sum);
                }
            }
            data.put("action_distribution", actionCounts);

            // Metrics
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("total_actions", totalSteps);
            long totalDuration = rec.getDuration() != null ? rec.getDuration() : 0;
            metrics.put("total_duration_ms", totalDuration);
            double avgDuration = totalSteps > 0 ? (double) totalDuration / totalSteps : 0;
            metrics.put("avg_step_duration", Math.round(avgDuration));
            metrics.put("max_step_duration", totalDuration);
            metrics.put("min_step_duration", 0);
            double actionsPerSec = totalDuration > 0 ? totalSteps * 1000.0 / totalDuration : 0;
            metrics.put("actions_per_second", Math.round(actionsPerSec * 100) / 100.0);
            data.put("metrics", metrics);

            data.put("bottlenecks", new ArrayList<>());
            data.put("recommendations", new ArrayList<>());

            r.put("data", data);
            return r;
        }).orElse(Map.of("success", false, "error", "not found"));
    }

    @GetMapping("/ai-execution-records/{id}/export-pdf/")
    public Map<String, Object> exportPdf(@PathVariable Long id) {
        Map<String, Object> reportData = report(id);
        if (Boolean.TRUE.equals(reportData.get("success"))) {
            reportData.put("export_type", "pdf");
            reportData.put("generated_at", LocalDateTime.now().toString());
        }
        return reportData;
    }

    // ── AI Cases ──
    @GetMapping("/ai-cases/")
    public Map<String, Object> listCases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(defaultValue = "") String search) {
        List<AiCase> all;
        if (search != null && !search.isEmpty()) {
            all = caseRepo.findAll().stream().filter(c ->
                (c.getName() != null && c.getName().contains(search)) ||
                (c.getDescription() != null && c.getDescription().contains(search))
            ).collect(Collectors.toList());
        } else {
            all = caseRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        int total = all.size();
        int from = Math.min((page - 1) * page_size, total);
        int to = Math.min(from + page_size, total);
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = from; i < to; i++) results.add(caseToMap(all.get(i)));
        Map<String, Object> r = new LinkedHashMap<>(); r.put("count", total); r.put("results", results);
        return r;
    }

    @GetMapping("/ai-cases/{id}/")
    public Map<String, Object> getCase(@PathVariable Long id) {
        return caseRepo.findById(id).map(this::caseToMap).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/ai-cases/")
    public Map<String, Object> createCase(@RequestBody Map<String, Object> body) {
        AiCase c = new AiCase();
        c.setName((String) body.getOrDefault("name", "未命名"));
        c.setDescription((String) body.getOrDefault("description", ""));
        c.setTaskDescription((String) body.getOrDefault("task_description", ""));
        c.setSteps(body.containsKey("steps") ? body.get("steps").toString() : null);
        c.setCreatedAt(LocalDateTime.now());
        c = caseRepo.save(c);
        return caseToMap(c);
    }

    @PatchMapping("/ai-cases/{id}/")
    public Map<String, Object> updateCase(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return caseRepo.findById(id).map(c -> {
            if (body.containsKey("name")) c.setName((String) body.get("name"));
            if (body.containsKey("description")) c.setDescription((String) body.get("description"));
            if (body.containsKey("task_description")) c.setTaskDescription((String) body.get("task_description"));
            if (body.containsKey("steps")) c.setSteps(body.get("steps").toString());
            c.setUpdatedAt(LocalDateTime.now());
            caseRepo.save(c);
            return caseToMap(c);
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/ai-cases/{id}/")
    public Map<String, Object> deleteCase(@PathVariable Long id) {
        caseRepo.deleteById(id);
        // Only delete steps that belong to this AI case
        stepRepo.deleteByTestCaseId(id);
        return Map.of("id", id, "message", "success");
    }

    @PostMapping("/ai-cases/{id}/run/")
    public Map<String, Object> runCase(@PathVariable Long id) {
        AiCase aiCase = caseRepo.findById(id).orElse(null);
        if (aiCase == null) return Map.of("error", "case not found");

        String taskDesc = aiCase.getTaskDescription();
        if (taskDesc == null || taskDesc.isBlank()) return Map.of("error", "task_description is empty");

        List<Map<String, Object>> plannedTasks = buildPlannedTasks(taskDesc);

        AiExecutionRecord r = new AiExecutionRecord();
        r.setCaseId(id);
        r.setCaseName(aiCase.getName());
        r.setTaskDescription(taskDesc);
        r.setStatus("running");
        r.setProgress(10);
        r.setResult("开始执行...");
        r.setSteps(toJson(plannedTasks));
        r.setStartTime(LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        r = repo.save(r);
        final Long recId = r.getId();
        final Long caseId = id;

        // Delete old steps for this AI case before re-recording
        stepRepo.deleteByTestCaseId(caseId);
        final java.util.concurrent.atomic.AtomicInteger stepOrder = new java.util.concurrent.atomic.AtomicInteger(1);

        Thread t = new Thread(() -> {
            try {
                runAgentAndSave(recId, taskDesc,
                    cmd -> {
                        TestCaseStep ts = new TestCaseStep();
                        ts.setTestCaseId(caseId);
                        ts.setStepOrder(stepOrder.getAndIncrement());
                        ts.setActionType(cmd.action());
                        ts.setSelector(cmd.selector());
                        ts.setInputValue(cmd.value());
                        ts.setOriginalInstruction(cmd.instruction());
                        ts.setElementTag(cmd.elementTag());
                        ts.setElementText(cmd.elementText());
                        ts.setPageUrl(cmd.pageUrl());
                        ts.setScreenshotPath(cmd.screenshotPath());
                        ts.setPlaywrightCode(cmd.playwrightCode());
                        ts.setCreatedAt(LocalDateTime.now());
                        stepRepo.save(ts);
                    });
            } finally {
                runningThreads.remove(recId);
            }
        });
        runningThreads.put(recId, t);
        t.start();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", r.getId()); resp.put("execution_id", r.getId());
        resp.put("status", "running"); resp.put("message", "AI任务已启动");
        return resp;
    }

    @PostMapping("/ai-cases/batch-run/")
    public Map<String, Object> batchRunCases(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.getOrDefault("ids", List.of());
        if (rawIds.isEmpty()) return Map.of("error", "ids is empty");

        List<Map<String, Object>> results = new ArrayList<>();
        for (Integer rid : rawIds) {
            Long caseId = rid.longValue();
            AiCase aiCase = caseRepo.findById(caseId).orElse(null);
            if (aiCase == null || aiCase.getTaskDescription() == null || aiCase.getTaskDescription().isBlank()) {
                results.add(Map.of("id", caseId, "status", "skipped", "reason", "empty"));
                continue;
            }
            // Create record and run in background
            List<Map<String, Object>> plannedTasks = buildPlannedTasks(aiCase.getTaskDescription());
            AiExecutionRecord r = new AiExecutionRecord();
            r.setCaseId(caseId);
            r.setCaseName(aiCase.getName());
            r.setTaskDescription(aiCase.getTaskDescription());
            r.setStatus("running");
            r.setProgress(10);
            r.setResult("开始执行...");
            r.setSteps(toJson(plannedTasks));
            r.setStartTime(LocalDateTime.now());
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            r = repo.save(r);
            final Long recId = r.getId();
            final Long cid = caseId;
            // Clear old steps + record new ones
            stepRepo.deleteByTestCaseId(cid);
            final java.util.concurrent.atomic.AtomicInteger order = new java.util.concurrent.atomic.AtomicInteger(1);
            new Thread(() -> runAgentAndSave(recId, aiCase.getTaskDescription(),
                cmd -> {
                    TestCaseStep ts = new TestCaseStep();
                    ts.setTestCaseId(cid);
                    ts.setStepOrder(order.getAndIncrement());
                    ts.setActionType(cmd.action());
                    ts.setSelector(cmd.selector());
                    ts.setInputValue(cmd.value());
                    ts.setOriginalInstruction(cmd.instruction());
                    ts.setElementTag(cmd.elementTag());
                    ts.setElementText(cmd.elementText());
                    ts.setPageUrl(cmd.pageUrl());
                    ts.setScreenshotPath(cmd.screenshotPath());
                    ts.setPlaywrightCode(cmd.playwrightCode());
                    ts.setCreatedAt(LocalDateTime.now());
                    stepRepo.save(ts);
                })).start();
            results.add(Map.of("id", caseId, "execution_id", recId, "status", "running"));
        }
        return Map.of("total", results.size(), "results", results);
    }

    @PostMapping("/ai-cases/polish/")
    public Map<String, Object> polishText(@RequestBody Map<String, Object> body) {
        String raw = (String) body.getOrDefault("text", "");
        if (raw.isBlank()) return Map.of("error", "text is empty");

        String result = miMoService.callAi(
            "把用户的话拆成操作步骤，每行一个，数字编号。有网址才放第一行。只输出步骤。",
            raw);
        if (result == null || result.isBlank()) return Map.of("polished", "");

        // Minimal cleanup — just normalize formatting
        String polished = result
            .replaceAll("(?m)^\\s*URL\\s*$", "")          // remove bare "URL" placeholder
            .replaceAll("\\n{3,}", "\n\n")                 // collapse excessive blank lines
            .replaceAll("(\\d+)[、.]\\s*", "\n$1、")      // ensure each step on its own line
            .replaceFirst("^\\n+", "")                     // strip leading newlines
            .trim();
        return Map.of("polished", polished);
    }

    // ── Prompt Templates CRUD ──
    @GetMapping("/prompts/")
    public Map<String, Object> listPrompts(@RequestParam(defaultValue = "") String type) {
        List<AiPromptTemplate> list = (type != null && !type.isEmpty())
            ? promptRepo.findByTypeOrderByCreatedAtDesc(type)
            : promptRepo.findByIsActiveTrueOrderByCreatedAtDesc();
        List<Map<String, Object>> r = new ArrayList<>();
        for (AiPromptTemplate p : list) r.add(promptToMap(p));
        return Map.of("count", r.size(), "results", r);
    }

    @PostMapping("/prompts/")
    public Map<String, Object> createPrompt(@RequestBody Map<String, Object> body) {
        AiPromptTemplate p = new AiPromptTemplate();
        p.setName((String) body.getOrDefault("name", "未命名"));
        p.setContent((String) body.getOrDefault("content", ""));
        p.setType((String) body.getOrDefault("type", "system"));
        p.setIsActive(true);
        p = promptRepo.save(p);
        return promptToMap(p);
    }

    @PatchMapping("/prompts/{id}/")
    public Map<String, Object> updatePrompt(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return promptRepo.findById(id).map(p -> {
            if (body.containsKey("name")) p.setName((String) body.get("name"));
            if (body.containsKey("content")) p.setContent((String) body.get("content"));
            if (body.containsKey("type")) p.setType((String) body.get("type"));
            if (body.containsKey("is_active")) p.setIsActive((Boolean) body.get("is_active"));
            p.setUpdatedAt(LocalDateTime.now());
            promptRepo.save(p);
            return promptToMap(p);
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/prompts/{id}/")
    public Map<String, Object> deletePrompt(@PathVariable Long id) {
        promptRepo.deleteById(id);
        return Map.of("message", "success");
    }

    private Map<String, Object> promptToMap(AiPromptTemplate p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName());
        m.put("content", p.getContent()); m.put("type", p.getType());
        m.put("is_active", p.getIsActive());
        m.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return m;
    }

    @PostMapping("/ai-cases/{id}/replay/")
    public Map<String, Object> replayCase(@PathVariable Long id) {
        boolean ok = replayService.replayTestCase(id);
        return Map.of("id", id, "success", ok, "message", ok ? "回放成功" : "回放失败，请查看服务端日志");
    }

    @PostMapping("/auth/generate-login-state/")
    public Map<String, Object> generateLoginState(@RequestBody Map<String, Object> body) {
        String loginUrl = (String) body.getOrDefault("login_url", "https://192.168.6.171:8088/#/login");
        String username = (String) body.getOrDefault("username", "admin");
        String password = (String) body.getOrDefault("password", "Aa123456");
        try {
            automationEngine.generateGlobalLoginState(loginUrl, username, password);
            return Map.of("success", true, "message", "登录状态已保存至 auth.json");
        } catch (Exception e) {
            return Map.of("success", false, "message", "生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/ai-cases/{id}/steps/")
    public Map<String, Object> listSteps(@PathVariable Long id) {
        List<TestCaseStep> steps = stepRepo.findByTestCaseIdOrderByStepOrderAsc(id);
        List<Map<String, Object>> list = new ArrayList<>();
        for (TestCaseStep s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId()); m.put("test_case_id", s.getTestCaseId());
            m.put("step_order", s.getStepOrder()); m.put("action_type", s.getActionType());
            m.put("selector", s.getSelector()); m.put("input_value", s.getInputValue());
            m.put("original_instruction", s.getOriginalInstruction());
            m.put("element_tag", s.getElementTag()); m.put("element_text", s.getElementText());
            m.put("page_url", s.getPageUrl()); m.put("playwright_code", s.getPlaywrightCode());
            m.put("created_at", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            list.add(m);
        }
        return Map.of("count", list.size(), "steps", list);
    }

    @GetMapping("/ai-cases/steps/all/")
    public Map<String, Object> listAllSteps() {
        List<TestCaseStep> all = stepRepo.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (TestCaseStep s : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId()); m.put("test_case_id", s.getTestCaseId());
            m.put("step_order", s.getStepOrder()); m.put("action_type", s.getActionType());
            m.put("selector", s.getSelector()); m.put("input_value", s.getInputValue());
            m.put("original_instruction", s.getOriginalInstruction());
            m.put("element_tag", s.getElementTag()); m.put("element_text", s.getElementText());
            m.put("page_url", s.getPageUrl()); m.put("playwright_code", s.getPlaywrightCode());
            m.put("created_at", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            // Look up case name
            if (s.getTestCaseId() != null) {
                caseRepo.findById(s.getTestCaseId()).ifPresent(c -> m.put("case_name", c.getName()));
            }
            list.add(m);
        }
        // Most recent first
        java.util.Collections.reverse(list);
        return Map.of("count", list.size(), "steps", list);
    }

    @DeleteMapping("/test-steps/by-case/{caseId}/")
    public Map<String, Object> deleteStepsByCase(@PathVariable Long caseId) {
        stepRepo.deleteByTestCaseId(caseId);
        return Map.of("message", "success");
    }

    @PatchMapping("/test-steps/{id}/")
    public Map<String, Object> updateStep(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return stepRepo.findById(id).map(s -> {
            if (body.containsKey("actionType")) s.setActionType((String) body.get("actionType"));
            if (body.containsKey("selector")) s.setSelector((String) body.get("selector"));
            if (body.containsKey("inputValue")) s.setInputValue((String) body.get("inputValue"));
            if (body.containsKey("step_order")) s.setStepOrder((Integer) body.get("step_order"));
            stepRepo.save(s);
            return Map.of("message", "success");
        }).orElse((Map) Map.of("error", "not found"));
    }

    // ═══════════════════ Internal ═══════════════════

    private List<Map<String, Object>> buildPlannedTasks(String taskDesc) {
        List<Map<String, Object>> plannedTasks = new ArrayList<>();
        if (taskDesc == null || taskDesc.isBlank()) return plannedTasks;

        // Try AI-based task planning first
        try {
            String aiResult = miMoService.callAi(
                "你是一个任务分解助手。将用户的任务描述分解为结构化的步骤列表。" +
                "返回纯JSON数组格式：[{\"id\":1,\"description\":\"步骤描述\",\"action\":\"操作类型\",\"target\":\"目标\"}]。" +
                "只返回JSON数组，不要其他内容。",
                taskDesc);
            if (aiResult != null && aiResult.trim().startsWith("[")) {
                String json = aiResult.trim();
                int end = json.lastIndexOf("]");
                if (end > 0) json = json.substring(0, end + 1);
                List<Map<String, Object>> aiTasks = mapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});
                for (Map<String, Object> t : aiTasks) {
                    if (!t.containsKey("status")) t.put("status", "pending");
                    if (!t.containsKey("id")) t.put("id", plannedTasks.size() + 1);
                    plannedTasks.add(t);
                }
                return plannedTasks;
            }
        } catch (Exception e) {
            // Fall back to line-based parsing
        }

        // Fallback: split by newlines
        for (String line : taskDesc.split("\n")) {
            line = line.trim().replaceAll("^\\d+[、.)]\\s*", "");
            if (line.isEmpty()) continue;
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("id", plannedTasks.size() + 1);
            task.put("description", line);
            task.put("status", "pending");
            plannedTasks.add(task);
        }
        return plannedTasks;
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    private void runAgentAndSave(Long recId, String taskDesc) {
        runAgentAndSave(recId, taskDesc, null);
    }

    private void runAgentAndSave(Long recId, String taskDesc,
                                  java.util.function.Consumer<BrowserAgentService.StepCommand> stepRecorder) {
        final int[] currentStep = {0};
        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            currentThread.interrupt();
        }, EXECUTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        try {
            BrowserAgentService.AgentResult agentResult = browserAgent.execute(taskDesc,
                line -> {
                    if (Thread.currentThread().isInterrupted()) return;
                    AiExecutionRecord rec = repo.findById(recId).orElse(null);
                    if (rec != null && !"stopped".equals(rec.getStatus())) {
                        String prev = rec.getResult() != null ? rec.getResult() : "";
                        rec.setResult(prev + (prev.isEmpty() ? "" : "\n") + line);
                        rec.setUpdatedAt(LocalDateTime.now());
                        if (rec.getStartTime() != null) {
                            rec.setDuration(Duration.between(rec.getStartTime(), LocalDateTime.now()).toMillis());
                        }

                        if (line.matches("^\\[\\d+/\\d+\\].*")) {
                            currentStep[0] = Integer.parseInt(line.replaceAll("^\\[(\\d+)/\\d+\\].*", "$1"));
                            int total = countLines(taskDesc);
                            rec.setProgress(Math.min(95, currentStep[0] * 100 / Math.max(1, total)));
                            updateStepStatus(rec, currentStep[0], "in_progress");
                        } else if (line.contains("  V ") && (line.contains("OK") || line.contains("NAV"))) {
                            updateStepStatus(rec, currentStep[0], "completed");
                        } else if (line.contains("  X FAILED")) {
                            updateStepStatus(rec, currentStep[0], "failed");
                        }
                        repo.save(rec);
                    }
                },
                stepRecorder);

            AiExecutionRecord rec = repo.findById(recId).orElse(null);
            if (rec != null && !"stopped".equals(rec.getStatus())) {
                rec.setStatus(agentResult.status());
                rec.setProgress(100);
                rec.setResult(agentResult.logs());
                try { rec.setScreenshots(mapper.writeValueAsString(agentResult.screenshots())); } catch (Exception ignored) {}
                if (rec.getStartTime() != null) {
                    rec.setDuration(Duration.between(rec.getStartTime(), LocalDateTime.now()).toMillis());
                }
                rec.setUpdatedAt(LocalDateTime.now());
                repo.save(rec);
            }
        } catch (Exception e) {
            AiExecutionRecord rec = repo.findById(recId).orElse(null);
            if (rec != null) {
                boolean wasInterrupted = Thread.currentThread().isInterrupted();
                rec.setStatus(wasInterrupted ? "stopped" : "failed");
                rec.setResult("执行" + (wasInterrupted ? "超时/中断" : "失败") + ": " + e.getMessage());
                rec.setUpdatedAt(LocalDateTime.now());
                if (rec.getStartTime() != null) {
                    rec.setDuration(Duration.between(rec.getStartTime(), LocalDateTime.now()).toMillis());
                }
                repo.save(rec);
            }
        } finally {
            timeoutFuture.cancel(false);
            scheduler.shutdownNow();
        }
    }

    private void updateStepStatus(AiExecutionRecord rec, int stepNum, String status) {
        if (rec.getSteps() == null || !rec.getSteps().startsWith("[")) return;
        try {
            List<Map<String, Object>> tasks = mapper.readValue(rec.getSteps(),
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});
            for (Map<String, Object> t : tasks) {
                Object idObj = t.get("id");
                if (idObj != null && String.valueOf(idObj).equals(String.valueOf(stepNum))) {
                    t.put("status", status);
                    break;
                }
            }
            rec.setSteps(mapper.writeValueAsString(tasks));
        } catch (Exception e) { /* silent */ }
    }

    private int countLines(String text) {
        return text == null || text.isEmpty() ? 1 : (int) text.lines().count();
    }

    private List<Map<String, Object>> parseSteps(String stepsJson) {
        if (stepsJson == null || !stepsJson.startsWith("[")) return new ArrayList<>();
        try {
            return mapper.readValue(stepsJson,
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format("%.1fs", ms / 1000.0);
        long minutes = ms / 60_000;
        long seconds = (ms % 60_000) / 1000;
        return String.format("%dm %ds", minutes, seconds);
    }

    private Map<String, Object> caseToMap(AiCase c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName()); m.put("description", c.getDescription());
        m.put("task_description", c.getTaskDescription()); m.put("steps", c.getSteps());
        m.put("status", c.getStatus());
        m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toMap(AiExecutionRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("case_id", r.getCaseId());
        m.put("case_name", r.getCaseName());
        m.put("task_description", r.getTaskDescription());
        m.put("status", r.getStatus());
        m.put("progress", r.getProgress());
        m.put("duration", r.getDuration() != null ? r.getDuration() / 1000.0 : null);
        m.put("start_time", r.getStartTime() != null ? r.getStartTime().toString() : null);
        m.put("result", r.getResult());
        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("updated_at", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);

        // Logs for frontend polling
        m.put("logs", r.getResult() != null ? r.getResult() : "");
        m.put("screenshots", r.getScreenshots() != null ? r.getScreenshots() : "[]");

        // Parse planned tasks - preserve actual per-step status
        List<Map<String, Object>> tasks = parseSteps(r.getSteps());
        // Only set remaining pending tasks to completed if overall succeeded (don't overwrite failed)
        if ("completed".equals(r.getStatus())) {
            for (Map<String, Object> t : tasks) {
                if ("pending".equals(t.get("status")) || "in_progress".equals(t.get("status"))) {
                    t.put("status", "completed");
                }
            }
        }
        m.put("planned_tasks", tasks);
        return m;
    }
}
