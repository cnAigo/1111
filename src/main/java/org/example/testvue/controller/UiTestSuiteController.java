package org.example.testvue.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.testvue.entity.*;
import org.example.testvue.repository.*;
import org.example.testvue.service.PlaywrightTestRunner;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation")
public class UiTestSuiteController {

    private final UiTestSuiteRepository repo;
    private final UiTestCaseRepository caseRepo;
    private final UiTestExecutionRepository execRepo;
    private final UiProjectRepository projectRepo;
    private final UiElementRepository elementRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public UiTestSuiteController(UiTestSuiteRepository repo, UiTestCaseRepository caseRepo,
                                  UiTestExecutionRepository execRepo, UiProjectRepository projectRepo,
                                  UiElementRepository elementRepo) {
        this.repo = repo; this.caseRepo = caseRepo; this.execRepo = execRepo;
        this.projectRepo = projectRepo; this.elementRepo = elementRepo;
    }

    // ── CRUD ──

    @GetMapping("/test-suites/")
    public Map<String, Object> list(@RequestParam(required = false) Long project) {
        List<UiTestSuite> all = (project != null) ? repo.findByProjectId(project) : repo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiTestSuite s : all) results.add(toMap(s));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size()); resp.put("results", results);
        return resp;
    }

    @GetMapping("/test-suites/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/test-suites/")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        UiTestSuite s = new UiTestSuite();
        updateFromBody(s, body);
        s.setTestCaseIds("[]");
        s.setCreatedAt(LocalDateTime.now()); s.setUpdatedAt(LocalDateTime.now());
        s = repo.save(s);
        return toMap(s);
    }

    @PatchMapping("/test-suites/{id}/")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(s -> {
            updateFromBody(s, body);
            s.setUpdatedAt(LocalDateTime.now());
            repo.save(s);
            return toMap(s);
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/test-suites/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    // ── Test Case management within suite ──

    @GetMapping("/test-suites/{id}/test_cases/")
    public List<Map<String, Object>> suiteTestCases(@PathVariable Long id) {
        UiTestSuite s = repo.findById(id).orElse(null);
        if (s == null) return List.of();
        List<Map<String, Object>> entries = parseCaseIds(s.getTestCaseIds());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            Long caseId = entry.get("id") instanceof Number ? ((Number) entry.get("id")).longValue() : null;
            if (caseId != null) {
                caseRepo.findById(caseId).ifPresent(tc -> {
                    Map<String, Object> tcMap = new LinkedHashMap<>();
                    tcMap.put("id", tc.getId()); tcMap.put("name", tc.getName());
                    tcMap.put("priority", tc.getPriority()); tcMap.put("status", tc.getStatus());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", caseId);
                    m.put("test_case", tcMap);
                    m.put("order", entry.getOrDefault("order", 0));
                    result.add(m);
                });
            }
        }
        result.sort(Comparator.comparingInt(o -> ((Number) o.getOrDefault("order", 0)).intValue()));
        return result;
    }

    @PostMapping("/test-suites/{id}/add_test_case/")
    public Map<String, Object> addTestCase(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(s -> {
            Long caseId = body.get("test_case_id") instanceof Number
                ? ((Number) body.get("test_case_id")).longValue() : null;
            if (caseId == null) return errorMap("test_case_id required");

            List<Map<String, Object>> entries = parseCaseIds(s.getTestCaseIds());
            int maxOrder = entries.stream().mapToInt(e -> ((Number) e.getOrDefault("order", 0)).intValue()).max().orElse(-1);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", caseId);
            entry.put("order", maxOrder + 1);
            entries.add(entry);
            try { s.setTestCaseIds(mapper.writeValueAsString(entries)); } catch (Exception ignored) {}
            s.setUpdatedAt(LocalDateTime.now());
            repo.save(s);
            return ok();
        }).orElse(errorMap("suite not found"));
    }

    @DeleteMapping("/test-suites/{suiteId}/remove_test_case/")
    public Map<String, Object> removeTestCase(@PathVariable Long suiteId, @RequestBody Map<String, Object> body) {
        return repo.findById(suiteId).map(s -> {
            Long caseId = body.get("test_case_id") instanceof Number
                ? ((Number) body.get("test_case_id")).longValue() : null;
            if (caseId == null) return errorMap("test_case_id required");

            List<Map<String, Object>> entries = parseCaseIds(s.getTestCaseIds());
            entries.removeIf(e -> caseId.equals(
                e.get("id") instanceof Number ? ((Number) e.get("id")).longValue() : null));
            try { s.setTestCaseIds(mapper.writeValueAsString(entries)); } catch (Exception ignored) {}
            s.setUpdatedAt(LocalDateTime.now());
            repo.save(s);
            return ok();
        }).orElse(errorMap("suite not found"));
    }

    @PostMapping("/test-suites/{suiteId}/update_test_case_order/")
    public Map<String, Object> updateTestOrder(@PathVariable Long suiteId, @RequestBody Map<String, Object> body) {
        return repo.findById(suiteId).map(s -> {
            Object orders = body.get("test_case_orders");
            if (orders instanceof List) {
                try { s.setTestCaseIds(mapper.writeValueAsString(orders)); } catch (Exception ignored) {}
                s.setUpdatedAt(LocalDateTime.now());
                repo.save(s);
            }
            return ok();
        }).orElse(errorMap("suite not found"));
    }

    // ── Run entire suite (Playwright) ──

    @PostMapping("/test-suites/{id}/run_suite/")
    public Map<String, Object> runSuite(@PathVariable Long id) {
        UiTestSuite s = repo.findById(id).orElse(null);
        if (s == null) return Map.of("success", false, "error", "Suite not found");

        String baseUrl = "";
        if (s.getProjectId() != null) {
            baseUrl = projectRepo.findById(s.getProjectId()).map(UiProject::getBaseUrl).orElse("");
        }
        if (baseUrl.isBlank()) return Map.of("success", false, "error", "请先配置项目 baseUrl");

        Map<Long, Map<String, Object>> elements = new LinkedHashMap<>();
        if (s.getProjectId() != null) {
            for (UiElement el : elementRepo.findByProjectId(s.getProjectId())) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("locator_value", el.getLocatorValue());
                info.put("locator_strategy", el.getLocatorStrategy());
                info.put("element_type", el.getElementType());
                info.put("wait_timeout", el.getWaitTimeout());
                info.put("force_action", el.getForceAction());
                elements.put(el.getId(), info);
            }
        }

        List<Map<String, Object>> entries = parseCaseIds(s.getTestCaseIds());
        entries.sort(Comparator.comparingInt(e -> ((Number) e.getOrDefault("order", 0)).intValue()));

        int totalPassed = 0, totalFailed = 0, totalCases = entries.size();
        List<Map<String, Object>> caseResults = new ArrayList<>();
        long startMs = System.currentTimeMillis();

        for (Map<String, Object> entry : entries) {
            Long caseId = entry.get("id") instanceof Number ? ((Number) entry.get("id")).longValue() : null;
            if (caseId == null) continue;

            UiTestCase tc = caseRepo.findById(caseId).orElse(null);
            if (tc == null) continue;

            StringBuilder logBuf = new StringBuilder();
            logBuf.append("=== Suite: ").append(s.getName()).append(" | Case: ").append(tc.getName()).append(" ===\n");
            Map<String, Object> runResult = PlaywrightTestRunner.execute(
                baseUrl, tc.getSteps(), elements, null,
                tc.getEngine() != null ? tc.getEngine() : "playwright",
                tc.getBrowser() != null ? tc.getBrowser() : "chromium",
                true, logBuf
            );

            UiTestExecution exec = new UiTestExecution();
            exec.setTestCaseId(tc.getId());
            exec.setTestCaseName(tc.getName());
            exec.setProjectId(tc.getProjectId());
            exec.setEngine(tc.getEngine()); exec.setBrowser(tc.getBrowser());
            exec.setStatus((String) runResult.get("status"));
            exec.setTotal((Integer) runResult.get("total"));
            exec.setPassed((Integer) runResult.get("passed"));
            exec.setFailed((Integer) runResult.get("failed"));
            exec.setDuration((String) runResult.get("duration"));
            exec.setLogs(logBuf.toString());
            exec.setScreenshots((String) runResult.get("screenshots"));
            exec.setExecutedAt(LocalDateTime.now());
            exec.setCreatedAt(LocalDateTime.now());
            execRepo.save(exec);

            totalPassed += (Integer) runResult.get("passed");
            totalFailed += (Integer) runResult.get("failed");
            caseResults.add(Map.of(
                "case_id", tc.getId(), "case_name", tc.getName(),
                "status", runResult.get("status"),
                "passed", runResult.get("passed"), "failed", runResult.get("failed")
            ));
        }

        long durationMs = System.currentTimeMillis() - startMs;
        String durationStr = durationMs >= 60000
            ? (durationMs / 60000) + "m" + ((durationMs % 60000) / 1000) + "s"
            : String.format("%.1fs", durationMs / 1000.0);

        return Map.of(
            "success", true,
            "status", totalFailed == 0 ? "passed" : "failed",
            "total_cases", totalCases, "passed_cases", totalPassed, "failed_cases", totalFailed,
            "duration", durationStr, "case_results", caseResults
        );
    }

    // ── helpers ──

    private Map<String, Object> toMap(UiTestSuite s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId()); m.put("name", s.getName());
        m.put("description", s.getDescription()); m.put("project_id", s.getProjectId());
        m.put("status", s.getStatus());
        List<Map<String, Object>> entries = parseCaseIds(s.getTestCaseIds());
        m.put("test_case_count", entries.size());
        m.put("test_case_ids", s.getTestCaseIds());
        m.put("created_at", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        m.put("updated_at", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        return m;
    }

    private void updateFromBody(UiTestSuite s, Map<String, Object> body) {
        if (body.containsKey("name")) s.setName((String) body.get("name"));
        if (body.containsKey("description")) s.setDescription((String) body.get("description"));
        // Accept both project_id and project (frontend sends "project")
        if (body.containsKey("project_id") && body.get("project_id") != null)
            s.setProjectId(Long.valueOf(body.get("project_id").toString()));
        else if (body.containsKey("project") && body.get("project") != null)
            s.setProjectId(Long.valueOf(body.get("project").toString()));
        if (body.containsKey("status")) s.setStatus((String) body.get("status"));
        if (body.containsKey("test_case_ids")) {
            Object t = body.get("test_case_ids");
            if (t instanceof String) s.setTestCaseIds((String) t);
            else if (t instanceof List) {
                try { s.setTestCaseIds(mapper.writeValueAsString(t)); } catch (Exception e) {}
            }
        }
    }

    private List<Map<String, Object>> parseCaseIds(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private Map<String, Object> ok() { return new LinkedHashMap<>(Map.of("success", true)); }
    private Map<String, Object> errorMap(String msg) { return new LinkedHashMap<>(Map.of("error", msg)); }
}
