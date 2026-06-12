package org.example.testvue.controller;

import org.example.testvue.dto.Dtos.*;
import org.example.testvue.entity.TestConfigEntity;
import org.example.testvue.entity.TestHistory;
import org.example.testvue.repository.TestConfigRepository;
import org.example.testvue.entity.TestCaseDetail;
import org.example.testvue.repository.TestCaseDetailRepository;
import org.example.testvue.repository.TestHistoryRepository;
import org.example.testvue.service.MavenTestRunner;
import org.example.testvue.service.SurefireParser;
import org.example.testvue.service.TestExecutionService;
import org.example.testvue.util.AESUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
public class TestRunnerController {

    private final TestExecutionService execService;
    private final MavenTestRunner mavenRunner;
    private final TestHistoryRepository historyRepo;
    private final TestConfigRepository configRepo;
    private final TestCaseDetailRepository caseDetailRepo;
    private final AESUtils aes;

    public TestRunnerController(TestExecutionService execService,
                                MavenTestRunner mavenRunner,
                                TestHistoryRepository historyRepo,
                                TestConfigRepository configRepo,
                                TestCaseDetailRepository caseDetailRepo,
                                AESUtils aes) {
        this.execService = execService;
        this.mavenRunner = mavenRunner;
        this.historyRepo = historyRepo;
        this.configRepo = configRepo;
        this.caseDetailRepo = caseDetailRepo;
        this.aes = aes;
    }

    // ── Run ──

    @PostMapping("/run")
    public Map<String, Object> runTests(@RequestBody TestRunRequest req) {
        String tid = execService.startRun(req);
        if (tid == null) {
            ApiResponse r = new ApiResponse(); r.code = 409; r.msg = "已有任务在执行中"; return r.toMap();
        }
        ApiResponse r = new ApiResponse(); r.taskId = tid; r.msg = "ok";
        r.label = req.testClass != null && !req.testClass.isBlank() ? req.testClass
                 : req.module != null && !req.module.isBlank() ? req.module : "ALL";
        return r.toMap();
    }

    @PostMapping("/stop/{taskId}")
    public Map<String, Object> stopTask(@PathVariable String taskId) {
        execService.stopRun();
        return Map.of("code", 200, "msg", "已停止");
    }

    @PostMapping("/rerun-failed")
    public Map<String, Object> rerunFailed(@RequestBody RerunRequest req) {
        // Find the failed task, extract failed class names, rerun only those
        TestHistory h = historyRepo.findByTaskId(req.taskId);
        if (h == null) {
            ApiResponse r = new ApiResponse(); r.code = 404; r.msg = "任务不存在"; return r.toMap();
        }
        Set<String> failedClasses = new LinkedHashSet<>();
        if (h.getResultJson() != null) {
            try {
                List<Map<String, Object>> results = new com.google.gson.Gson().fromJson(h.getResultJson(),
                    new com.google.gson.reflect.TypeToken<List<Map<String,Object>>>(){}.getType());
                for (Map<String, Object> cls : results) {
                    int f = ((Number)cls.getOrDefault("failures",0)).intValue() + ((Number)cls.getOrDefault("errors",0)).intValue();
                    if (f > 0) failedClasses.add((String)cls.get("className"));
                }
            } catch (Exception ignored) {}
        }
        if (failedClasses.isEmpty()) {
            ApiResponse r = new ApiResponse(); r.code = 200; r.msg = "没有失败的用例"; return r.toMap();
        }

        TestRunRequest rr = new TestRunRequest();
        rr.testClass = String.join(",", failedClasses);
        String tid = execService.startRun(rr);
        ApiResponse r = new ApiResponse();
        r.taskId = tid; r.msg = "已下发重跑"; r.label = "Rerun " + failedClasses.size() + " classes";
        r.rerunClassNames = new ArrayList<>(failedClasses);
        r.rerunClassCount = failedClasses.size();
        return r.toMap();
    }

    // ── Estimate ──

    @PostMapping("/estimate")
    public Map<String, Object> estimate(@RequestBody TestRunRequest req) {
        long ms = mavenRunner.calculateExpectedMs(req);
        return Map.of("estimatedMs", ms, "estimatedFmt", MavenTestRunner.fmt(ms));
    }

    // ── Status ──

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alive", true);
        m.put("status", execService.getCurrentStatus().status);
        m.put("isRunning", execService.isRunning());
        m.put("label", execService.getCurrentStatus().label);
        return m;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(@RequestParam(value = "taskId", required = false) String taskId) {
        StatusResponse s = execService.getCurrentStatus();
        if (taskId != null) s.taskId = taskId;
        return s.toMap();
    }

    // ── Results ──

    @GetMapping("/results")
    public List<ClassResult> testResults() {
        return SurefireParser.parseDir(Paths.get(System.getProperty("user.dir"), "target/surefire-reports"));
    }

    @GetMapping("/report-summary")
    public Map<String, Object> reportSummary() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Path p = Paths.get(System.getProperty("user.dir"), "target/site/allure-maven-plugin/widgets/summary.json");
            if (!Files.exists(p)) { m.put("error", "report not found"); return m; }
            var root = new com.google.gson.Gson().fromJson(Files.readString(p), com.google.gson.JsonObject.class);
            m.put("total",   root.has("total") ? root.get("total").getAsInt() : 0);
            m.put("passed",  root.has("passed") ? root.get("passed").getAsInt() : 0);
            int f = (root.has("failed") ? root.get("failed").getAsInt() : 0) + (root.has("broken") ? root.get("broken").getAsInt() : 0);
            m.put("failed",  f);
            m.put("skipped", root.has("skipped") ? root.get("skipped").getAsInt() : 0);
            m.put("timeSec", root.has("sumDuration") ? root.get("sumDuration").getAsLong() / 1000 : 0);
        } catch (Exception e) { m.put("error", e.getMessage()); }
        return m;
    }

    // ── History ──

    @GetMapping("/history")
    public List<HistoryItem> getHistory() {
        return historyRepo.findAllByOrderByCreateTimeDesc(PageRequest.of(0, 50)).stream().map(h -> {
            HistoryItem item = new HistoryItem();
            item.taskId = h.getTaskId(); item.label = h.getLabel(); item.status = h.getStatus();
            item.createTime = h.getCreateTime() != null ? h.getCreateTime().toString() : "";
            item.durationFmt = h.getDurationFmt();
            item.passed = h.getPassed(); item.failed = h.getFailed(); item.skipped = h.getSkipped();
            return item;
        }).collect(Collectors.toList());
    }

    @GetMapping("/history/{taskId}/cases")
    public List<Map<String, Object>> getTaskCases(@PathVariable String taskId) {
        TestHistory h = historyRepo.findByTaskId(taskId);
        if (h != null && h.getResultJson() != null) {
            try { return new com.google.gson.Gson().fromJson(h.getResultJson(),
                new com.google.gson.reflect.TypeToken<List<Map<String,Object>>>(){}.getType()); } catch (Exception ignored) {}
        }
        return Collections.emptyList();
    }

    @GetMapping("/history/{taskId}/log")
    public Map<String, Object> getHistoryLog(@PathVariable String taskId) {
        TestHistory h = historyRepo.findByTaskId(taskId);
        String log;
        if (h != null && h.getLogFilePath() != null) {
            // Read from disk log file
            log = MavenTestRunner.readLog(taskId);
            if (log.isEmpty()) log = h.getOutput();
        } else if (h != null) {
            log = h.getOutput();
        } else {
            log = "";
        }
        return Map.of("code", 200, "taskId", taskId, "output", log != null ? log : "");
    }

    @Transactional
    @DeleteMapping("/history/{taskId}")
    public Map<String, Object> deleteHistory(@PathVariable String taskId) {
        historyRepo.deleteByTaskId(taskId);
        return Map.of("code", 200, "msg", "ok");
    }

    @Transactional
    @DeleteMapping("/history")
    public Map<String, Object> deleteAllHistory() {
        historyRepo.deleteAll();
        return Map.of("code", 200, "msg", "ok");
    }

    // ── Failed cases ──

    @GetMapping("/failed-cases")
    public List<FailedCase> getFailedCases() {
        List<FailedCase> result = new ArrayList<>();
        for (TestHistory h : historyRepo.findAllByOrderByCreateTimeDesc(PageRequest.of(0, 50))) {
            if (!"FAILED".equals(h.getStatus()) || h.getResultJson() == null) continue;
            try {
                List<Map<String, Object>> classes = new com.google.gson.Gson().fromJson(h.getResultJson(),
                    new com.google.gson.reflect.TypeToken<List<Map<String,Object>>>(){}.getType());
                for (Map<String, Object> cls : classes) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> clsCases = (List<Map<String, String>>) cls.get("cases");
                    if (clsCases == null) continue;
                    for (Map<String, String> c : clsCases) {
                        if ("FAIL".equals(c.get("status"))) {
                            FailedCase fc = new FailedCase();
                            fc.className = (String)cls.get("className");
                            fc.methodName = c.get("name");
                            fc.reason = c.get("reason");
                            fc.lastFailTime = h.getCreateTime() != null ? h.getCreateTime().toString() : "";
                            result.add(fc);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    // ── Configs ──

    @GetMapping("/configs")
    public List<ConfigItem> getConfigs() {
        List<ConfigItem> list = new ArrayList<>();
        for (TestConfigEntity c : configRepo.findAll()) {
            ConfigItem item = new ConfigItem();
            item.id = c.getId(); item.configName = c.getConfigName();
            item.url = c.getUrl(); item.projectId = c.getProjectId();
            item.username = c.getUsername();
            // Return masked password — never expose ciphertext or plaintext to frontend
            item.password = AESUtils.mask(c.getPassword());
            list.add(item);
        }
        return list;
    }

    @Transactional
    @PostMapping("/configs")
    public Map<String, Object> saveConfig(@RequestBody ConfigSaveRequest body) {
        String password = body.password;
        // If frontend sends masked placeholder, keep empty (= no password update)
        if (password != null && password.equals(AESUtils.mask(null))) {
            password = ""; // treat as "no change" — encrypting empty yields empty
        }
        String encryptedPassword = password != null && !password.isEmpty()
            ? aes.encrypt(password) : "";
        TestConfigEntity c = new TestConfigEntity(
            body.configName, body.url, body.projectId, body.username, encryptedPassword);
        c = configRepo.save(c);
        return Map.of("code", 200, "msg", "ok", "id", c.getId());
    }

    @DeleteMapping("/configs/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        configRepo.deleteById(id);
        return Map.of("code", 200, "msg", "ok");
    }

    // ── Test Case Details ──

    @GetMapping("/case-details")
    public List<Map<String, Object>> getCaseDetails(@RequestParam(defaultValue = "") String className) {
        List<TestCaseDetail> list = className.isBlank()
            ? caseDetailRepo.findAll()
            : caseDetailRepo.findByClassName(className);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TestCaseDetail d : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("caseId", d.getCaseId()); m.put("module", d.getModule());
            m.put("title", d.getTitle()); m.put("caseType", d.getCaseType());
            m.put("steps", d.getSteps()); m.put("expected", d.getExpected());
            m.put("apiUrl", d.getApiUrl()); m.put("httpMethod", d.getHttpMethod());
            m.put("javaMethod", d.getJavaMethod()); m.put("className", d.getClassName());
            result.add(m);
        }
        return result;
    }

    @Transactional
    @PostMapping("/case-details/import")
    public Map<String, Object> importCaseDetails(@RequestBody List<Map<String, String>> cases) {
        List<TestCaseDetail> entities = new ArrayList<>();
        for (Map<String, String> c : cases) {
            entities.add(new TestCaseDetail()
                .setCaseId(c.get("caseId")).setModule(c.get("module"))
                .setTitle(c.get("title")).setCaseType(c.get("caseType"))
                .setSteps(c.get("steps")).setExpected(c.get("expected"))
                .setApiUrl(c.get("apiUrl")).setHttpMethod(c.get("httpMethod"))
                .setJavaMethod(c.get("javaMethod")).setClassName(c.get("className")));
        }
        caseDetailRepo.saveAll(entities);
        return Map.of("code", 200, "msg", "ok", "count", entities.size());
    }

    // ── Cleanup ──

    @PostMapping("/cleanup")
    public Map<String, Object> cleanup(@RequestBody(required = false) Map<String, String> body) {
        String projectId = body != null ? body.getOrDefault("projectId", "") : "";
        String tid = execService.startCleanup(projectId,
            body != null ? body.getOrDefault("url", "") : "",
            body != null ? body.getOrDefault("username", "") : "",
            body != null ? body.getOrDefault("password", "") : "");
        if (tid == null) {
            ApiResponse r = new ApiResponse(); r.code = 409; r.msg = "已有任务在执行中"; return r.toMap();
        }
        return Map.of("code", 200, "msg", "ok", "taskId", tid);
    }
}
