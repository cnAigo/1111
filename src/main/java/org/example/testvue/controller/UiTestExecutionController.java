package org.example.testvue.controller;

import org.example.testvue.entity.UiTestExecution;
import org.example.testvue.repository.UiTestExecutionRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ui-automation")
public class UiTestExecutionController {

    private final UiTestExecutionRepository repo;

    public UiTestExecutionController(UiTestExecutionRepository repo) { this.repo = repo; }

    @GetMapping("/test-executions/")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String browser) {
        List<UiTestExecution> all = repo.findAllByOrderByExecutedAtDesc();
        // Apply filters
        if (search != null && !search.isEmpty()) {
            all = all.stream().filter(e ->
                (e.getTestCaseName() != null && e.getTestCaseName().contains(search)) ||
                (e.getLogs() != null && e.getLogs().contains(search))
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            all = all.stream().filter(e -> status.equalsIgnoreCase(e.getStatus())).collect(Collectors.toList());
        }
        if (browser != null && !browser.isEmpty()) {
            all = all.stream().filter(e -> browser.equalsIgnoreCase(e.getBrowser())).collect(Collectors.toList());
        }

        int total = all.size();
        int from = Math.min((page - 1) * page_size, total);
        int to = Math.min(from + page_size, total);
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Map<String, Object> m = toMap(all.get(i));
            // Trim large logs for list view
            if (m.get("logs") != null) {
                String logs = (String) m.get("logs");
                if (logs.length() > 200) m.put("logs", logs.substring(0, 200) + "...");
            }
            m.put("screenshots", null); // skip in list
            results.add(m);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", total); resp.put("results", results);
        return resp;
    }

    @GetMapping("/test-executions/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/test-executions/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    @PostMapping("/test-executions/batch-delete/")
    public Map<String, Object> batchDelete(@RequestBody Map<String, Object> body) {
        Object ids = body.get("ids");
        if (ids instanceof List) {
            for (Object id : (List<?>) ids) {
                repo.deleteById(Long.valueOf(id.toString()));
            }
        }
        return Map.of("message", "success");
    }

    private Map<String, Object> toMap(UiTestExecution e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("test_case_id", e.getTestCaseId());
        m.put("test_case", Map.of("id", e.getTestCaseId() != null ? e.getTestCaseId() : 0, "name", e.getTestCaseName()));
        m.put("test_case_name", e.getTestCaseName());
        m.put("status", e.getStatus());
        m.put("engine", e.getEngine());
        m.put("browser", e.getBrowser());
        m.put("headless", true);
        m.put("passed", e.getPassed());
        m.put("failed", e.getFailed());
        m.put("total", e.getTotal());
        m.put("duration", e.getDuration());
        m.put("logs", e.getLogs());
        m.put("screenshots", e.getScreenshots());
        m.put("project_id", e.getProjectId());
        m.put("error_message", "failed".equals(e.getStatus()) ? e.getLogs() : "");
        m.put("execution_logs", e.getLogs() != null ? e.getLogs() : "");
        m.put("started_at", e.getExecutedAt() != null ? e.getExecutedAt().toString() : null);
        m.put("finished_at", e.getExecutedAt() != null ? e.getExecutedAt().toString() : null);
        m.put("executed_at", e.getExecutedAt() != null ? e.getExecutedAt().toString() : null);
        m.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return m;
    }
}
