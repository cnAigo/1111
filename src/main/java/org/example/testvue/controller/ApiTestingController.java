package org.example.testvue.controller;

import com.google.gson.*;
import org.example.testvue.entity.*;
import org.example.testvue.repository.*;
import org.example.testvue.service.ApiExecutionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/api-testing")
public class ApiTestingController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApiProjectRepository projectRepo;
    private final ApiCollectionRepository collRepo;
    private final ApiRequestRepository reqRepo;
    private final ApiEnvironmentRepository envRepo;
    private final ApiTestSuiteRepository suiteRepo;
    private final ApiTestExecutionRepository execRepo;
    private final ApiRequestHistoryRepository histRepo;
    private final ApiScheduledTaskRepository taskRepo;
    private final AIServiceConfigRepository aiRepo;
    private final NotificationConfigRepository notifRepo;
    private final ApiExecutionService execService;
    private final ApiTestSuiteRequestRepository suiteReqRepo;

    public ApiTestingController(ApiProjectRepository projectRepo, ApiCollectionRepository collRepo,
                                ApiRequestRepository reqRepo, ApiEnvironmentRepository envRepo,
                                ApiTestSuiteRepository suiteRepo, ApiTestExecutionRepository execRepo,
                                ApiRequestHistoryRepository histRepo, ApiScheduledTaskRepository taskRepo,
                                AIServiceConfigRepository aiRepo, NotificationConfigRepository notifRepo,
                                ApiExecutionService execService, ApiTestSuiteRequestRepository suiteReqRepo) {
        this.projectRepo = projectRepo;
        this.collRepo = collRepo;
        this.reqRepo = reqRepo;
        this.envRepo = envRepo;
        this.suiteRepo = suiteRepo;
        this.execRepo = execRepo;
        this.histRepo = histRepo;
        this.taskRepo = taskRepo;
        this.aiRepo = aiRepo;
        this.notifRepo = notifRepo;
        this.execService = execService;
        this.suiteReqRepo = suiteReqRepo;
    }

    // ═══════════════════ Dashboard ═══════════════════
    @GetMapping("/dashboard/stats/")
    public Map<String, Object> dashboardStats() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("project_count", projectRepo.count());
        d.put("interface_count", reqRepo.count());
        d.put("suite_count", suiteRepo.count());
        d.put("execution_count", execRepo.count());
        d.put("history_count", histRepo.count());
        return d;
    }

    // ═══════════════════ Collections ═══════════════════
    @GetMapping("/collections/")
    public List<Map<String, Object>> listCollections(@RequestParam(name = "project", required = false) Long projectId) {
        List<ApiCollection> list;
        if (projectId != null) {
            list = collRepo.findByProjectIdOrderBySortOrder(projectId);
        } else {
            list = collRepo.findAll();
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (ApiCollection c : list) {
            r.add(toMap(c));
        }
        return r;
    }

    @GetMapping("/collections/search")
    public List<Map<String, Object>> searchCollections(@RequestParam(defaultValue = "") String q) {
        List<ApiCollection> list;
        if (q.isEmpty()) {
            list = collRepo.findAll();
        } else {
            list = collRepo.findByNameContainingIgnoreCase(q);
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (ApiCollection c : list) {
            r.add(toMap(c));
        }
        return r;
    }

    @PostMapping("/collections/")
    @Transactional
    public Map<String, Object> createCollection(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        ApiCollection c = new ApiCollection();
        c.setName(name);
        c.setDescription(str(body, "description"));
        c.setParentId(lng(body, "parent"));
        c.setProjectId(lng(body, "project_id"));
        c.setSortOrder(intVal(body, "sort_order", 0));
        c = collRepo.save(c);
        Map<String, Object> r = toMap(c);
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/collections/{id}/")
    @Transactional
    public Map<String, Object> updateCollection(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiCollection c = collRepo.findById(id).orElse(null);
        if (c == null) return err("Not found");
        if (body.containsKey("name")) c.setName(str(body, "name"));
        if (body.containsKey("description")) c.setDescription(str(body, "description"));
        if (body.containsKey("parent")) c.setParentId(lng(body, "parent"));
        if (body.containsKey("project_id")) c.setProjectId(lng(body, "project_id"));
        c.setUpdatedAt(LocalDateTime.now());
        c = collRepo.save(c);
        Map<String, Object> r = toMap(c);
        r.put("message", "success");
        return r;
    }

    @DeleteMapping("/collections/{id}/")
    @Transactional
    public Map<String, Object> deleteCollection(@PathVariable Long id) {
        reqRepo.deleteByCollectionId(id);
        collRepo.deleteById(id);
        return ok();
    }

    // ═══════════════════ API Requests ═══════════════════
    @GetMapping("/requests/")
    public Map<String, Object> listRequests(@RequestParam(defaultValue = "") Long collectionId) {
        List<ApiRequest> list;
        if (collectionId != null) {
            list = reqRepo.findByCollectionIdOrderBySortOrder(collectionId);
        } else {
            list = reqRepo.findAll();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiRequest r : list) {
            results.add(reqToMap(r));
        }
        return paginated(results);
    }

    @GetMapping("/requests/{id}/")
    public Map<String, Object> getRequest(@PathVariable Long id) {
        ApiRequest r = reqRepo.findById(id).orElse(null);
        if (r == null) return err("Not found");
        return reqToMapFull(r);
    }

    @PostMapping("/requests/")
    @Transactional
    public Map<String, Object> createRequest(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        ApiRequest r = new ApiRequest();
        applyRequestFields(r, body);
        r = reqRepo.save(r);
        Map<String, Object> result = reqToMapFull(r);
        result.put("message", "success");
        return result;
    }

    @PatchMapping("/requests/{id}/")
    @Transactional
    public Map<String, Object> updateRequest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiRequest r = reqRepo.findById(id).orElse(null);
        if (r == null) return err("Not found");
        applyRequestFields(r, body);
        r.setUpdatedAt(LocalDateTime.now());
        r = reqRepo.save(r);
        Map<String, Object> result = reqToMapFull(r);
        result.put("message", "success");
        return result;
    }

    @DeleteMapping("/requests/{id}/")
    @Transactional
    public Map<String, Object> deleteRequest(@PathVariable Long id) {
        histRepo.deleteByRequestId(id);
        reqRepo.deleteById(id);
        return ok();
    }

    @PostMapping("/api-requests/{id}/execute/")
    public Map<String, Object> executeApiRequest(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return doExecuteRequest(id, body);
    }

    @PostMapping("/requests/{id}/execute/")
    public Map<String, Object> executeRequestDirect(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return doExecuteRequest(id, body);
    }

    private Map<String, Object> doExecuteRequest(Long id, Map<String, Object> body) {
        ApiRequest req = reqRepo.findById(id).orElse(null);
        String method, url, headers, params, reqBody, bodyType;
        if (req != null) {
            method = req.getMethod();
            url = req.getUrl();
            headers = req.getHeaders();
            params = req.getParams();
            reqBody = req.getBody();
            bodyType = req.getBodyType();
        } else if (body != null) {
            method = str(body, "method", "GET");
            url = str(body, "url", "");
            headers = str(body, "headers");
            params = str(body, "params");
            reqBody = str(body, "body");
            bodyType = str(body, "bodyType", "none");
        } else {
            return Map.of("status_code", 0, "response_time", "0ms", "body", "{\"error\":\"Request not found\"}");
        }

        // Apply environment base_url substitution
        if (body != null && body.containsKey("environment_id")) {
            Long envId = lng(body, "environment_id");
            if (envId != null) {
                ApiEnvironment env = envRepo.findById(envId).orElse(null);
                if (env != null && env.getBaseUrl() != null && !env.getBaseUrl().isBlank()) {
                    String base = env.getBaseUrl().replaceAll("/+$", "");
                    if (!url.startsWith("http")) url = base + (url.startsWith("/") ? url : "/" + url);
                }
            }
        }

        Map<String, Object> result = execService.execute(method, url, headers, params, reqBody, bodyType);

        // Save history
        ApiRequestHistory hist = new ApiRequestHistory();
        hist.setRequestId(id);
        hist.setRequestName(req != null ? req.getName() : "Ad-hoc");
        hist.setMethod(method);
        hist.setUrl(url);
        hist.setRequestHeaders(headers);
        hist.setRequestParams(params);
        hist.setRequestBody(reqBody);
        Object statusCode = result.get("status_code");
        hist.setStatusCode(statusCode instanceof Number n ? n.intValue() : 0);
        hist.setResponseTime((String) result.get("response_time"));
        hist.setResponseHeaders((String) result.getOrDefault("headers", ""));
        hist.setResponseBody((String) result.getOrDefault("body", ""));
        histRepo.save(hist);

        // Add response_data wrapper for frontend compatibility
        Map<String, Object> responseData = new LinkedHashMap<>();
        String respBody = (String) result.getOrDefault("body", "");
        try {
            responseData.put("json", new Gson().fromJson(respBody, Object.class));
        } catch (Exception e) {
            responseData.put("body", respBody);
        }
        result.put("response_data", responseData);

        return result;
    }

    // ═══════════════════ Environments ═══════════════════
    @GetMapping("/environments/")
    public List<Map<String, Object>> listEnvironments(@RequestParam(name = "project", required = false) Long projectId) {
        List<ApiEnvironment> list;
        if (projectId != null) {
            list = envRepo.findByProjectId(projectId);
        } else {
            list = envRepo.findAll();
        }
        List<Map<String, Object>> r = new ArrayList<>();
        for (ApiEnvironment e : list) {
            r.add(envToMap(e));
        }
        return r;
    }

    @PostMapping("/environments/")
    @Transactional
    public Map<String, Object> createEnvironment(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        ApiEnvironment e = new ApiEnvironment();
        applyEnvFields(e, body);
        e = envRepo.save(e);
        Map<String, Object> r = envToMap(e);
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/environments/{id}/")
    @Transactional
    public Map<String, Object> updateEnvironment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiEnvironment e = envRepo.findById(id).orElse(null);
        if (e == null) return err("Not found");
        applyEnvFields(e, body);
        e.setUpdatedAt(LocalDateTime.now());
        e = envRepo.save(e);
        Map<String, Object> r = envToMap(e);
        r.put("message", "success");
        return r;
    }

    @DeleteMapping("/environments/{id}/")
    @Transactional
    public Map<String, Object> deleteEnvironment(@PathVariable Long id) {
        envRepo.deleteById(id);
        return ok();
    }

    @PostMapping("/environments/{id}/activate/")
    public Map<String, Object> activateEnvironment(@PathVariable Long id) {
        ApiEnvironment e = envRepo.findById(id).orElse(null);
        if (e == null) return err("Not found");
        return Map.of("message", "success", "environment", envToMap(e));
    }

    // ═══════════════════ Request History ═══════════════════
    @GetMapping("/histories/")
    public Map<String, Object> listHistory(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int page_size,
                                           @RequestParam(defaultValue = "") String search,
                                           @RequestParam(defaultValue = "") String request__request_type) {
        List<ApiRequestHistory> list = histRepo.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiRequestHistory h : list) {
            // Filter by search
            if (!search.isEmpty()) {
                String lower = search.toLowerCase();
                if (!(h.getRequestName() != null && h.getRequestName().toLowerCase().contains(lower))
                    && !(h.getUrl() != null && h.getUrl().toLowerCase().contains(lower))) continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("request_id", h.getRequestId());
            m.put("request_name", h.getRequestName());
            m.put("method", h.getMethod());
            m.put("url", h.getUrl());
            m.put("status_code", h.getStatusCode());
            m.put("response_time", h.getResponseTime());
            m.put("created_at", h.getCreatedAt() != null ? h.getCreatedAt().format(DT) : null);
            // Nested request object
            Map<String, Object> reqObj = new LinkedHashMap<>();
            reqObj.put("id", h.getRequestId());
            reqObj.put("name", h.getRequestName());
            reqObj.put("method", h.getMethod());
            m.put("request", reqObj);
            // request_data with full detail
            Map<String, Object> reqData = new LinkedHashMap<>();
            reqData.put("url", h.getUrl());
            reqData.put("headers", parseJson(h.getRequestHeaders()));
            reqData.put("params", parseJson(h.getRequestParams()));
            reqData.put("body", parseJson(h.getRequestBody()));
            m.put("request_data", reqData);
            // response_data
            Map<String, Object> respData = new LinkedHashMap<>();
            respData.put("status_code", h.getStatusCode());
            respData.put("response_time", h.getResponseTime());
            respData.put("headers", parseJson(h.getResponseHeaders()));
            respData.put("body", parseJson(h.getResponseBody()));
            m.put("response_data", respData);
            m.put("executed_by", Map.of("username", "admin"));
            m.put("executed_at", h.getCreatedAt() != null ? h.getCreatedAt().format(DT) : null);
            results.add(m);
        }
        return paginated(results);
    }

    @GetMapping("/histories/{id}/")
    public Map<String, Object> getHistory(@PathVariable Long id) {
        ApiRequestHistory h = histRepo.findById(id).orElse(null);
        if (h == null) return err("Not found");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", h.getId());
        m.put("request_id", h.getRequestId());
        m.put("request_name", h.getRequestName());
        m.put("method", h.getMethod());
        m.put("url", h.getUrl());
        m.put("request_headers", h.getRequestHeaders());
        m.put("request_body", h.getRequestBody());
        m.put("status_code", h.getStatusCode());
        m.put("response_time", h.getResponseTime());
        m.put("response_headers", h.getResponseHeaders());
        m.put("response_body", h.getResponseBody());
        m.put("created_at", h.getCreatedAt() != null ? h.getCreatedAt().format(DT) : null);
        return m;
    }

    @DeleteMapping("/histories/{id}/")
    @Transactional
    public Map<String, Object> deleteHistory(@PathVariable Long id) {
        histRepo.deleteById(id);
        return ok();
    }

    @PostMapping("/histories/batch-delete/")
    @Transactional
    public Map<String, Object> batchDeleteHistory(@RequestBody Map<String, Object> body) {
        Object idsObj = body.get("ids");
        if (idsObj instanceof List<?> rawList) {
            List<Long> ids = rawList.stream()
                .map(o -> o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()))
                .toList();
            for (Long id : ids) histRepo.deleteById(id);
        }
        return ok();
    }

    // ═══════════════════ Test Suites ═══════════════════
    @GetMapping("/test-suites/")
    public Map<String, Object> listSuites(@RequestParam(name = "project", required = false) Long projectId) {
        List<ApiTestSuite> list;
        if (projectId != null) {
            list = suiteRepo.findByProjectId(projectId);
        } else {
            list = suiteRepo.findAll();
        }
        // Batch load request counts to avoid N+1
        Map<Long, Long> countsBySuite = new HashMap<>();
        for (ApiTestSuite s : list) {
            countsBySuite.put(s.getId(), suiteReqRepo.countByTestSuiteId(s.getId()));
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiTestSuite s : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId()); m.put("name", s.getName());
            m.put("description", s.getDescription());
            m.put("request_count", countsBySuite.getOrDefault(s.getId(), 0L).intValue());
            m.put("created_at", s.getCreatedAt() != null ? s.getCreatedAt().format(DT) : null);
            results.add(m);
        }
        return paginated(results);
    }

    @PostMapping("/test-suites/")
    @Transactional
    public Map<String, Object> createSuite(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        ApiTestSuite s = new ApiTestSuite();
        s.setName(name);
        s.setDescription(str(body, "description"));
        s.setRequestIds(str(body, "request_ids"));
        Long pid = lng(body, "project_id");
        if (pid == null) pid = lng(body, "project");
        s.setProjectId(pid);
        s = suiteRepo.save(s);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", s.getId()); r.put("name", s.getName());
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/test-suites/{id}/")
    @Transactional
    public Map<String, Object> updateSuite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiTestSuite s = suiteRepo.findById(id).orElse(null);
        if (s == null) return err("Not found");
        if (body.containsKey("name")) s.setName(str(body, "name"));
        if (body.containsKey("description")) s.setDescription(str(body, "description"));
        if (body.containsKey("request_ids")) s.setRequestIds(str(body, "request_ids"));
        if (body.containsKey("project_id")) s.setProjectId(lng(body, "project_id"));
        if (body.containsKey("project")) s.setProjectId(lng(body, "project"));
        s.setUpdatedAt(LocalDateTime.now());
        s = suiteRepo.save(s);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", s.getId()); r.put("name", s.getName());
        r.put("message", "success");
        return r;
    }

    @DeleteMapping("/test-suites/{id}/")
    @Transactional
    public Map<String, Object> deleteSuite(@PathVariable Long id) {
        suiteReqRepo.deleteByTestSuiteId(id);
        execRepo.deleteBySuiteId(id);
        suiteRepo.deleteById(id);
        return ok();
    }

    // ═══════════════ Suite Request Management ═══════════════

    @GetMapping("/test-suites/{id}/requests/")
    public List<Map<String, Object>> listSuiteRequests(@PathVariable Long id) {
        List<ApiTestSuiteRequest> list = suiteReqRepo.findByTestSuiteIdOrderByOrderNo(id);
        // Batch load all requests to avoid N+1
        List<Long> requestIds = list.stream().map(ApiTestSuiteRequest::getRequestId).toList();
        Map<Long, ApiRequest> reqMap = reqRepo.findAllById(requestIds).stream()
            .collect(Collectors.toMap(ApiRequest::getId, r -> r));

        List<Map<String, Object>> r = new ArrayList<>();
        for (ApiTestSuiteRequest sr : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sr.getId()); m.put("test_suite_id", sr.getTestSuiteId());
            m.put("request_id", sr.getRequestId()); m.put("order", sr.getOrderNo());
            m.put("enabled", sr.getEnabled()); m.put("assertions", parseJson(sr.getAssertions()));
            ApiRequest req = reqMap.get(sr.getRequestId());
            if (req != null) {
                m.put("request_name", req.getName()); m.put("method", req.getMethod());
                m.put("url", req.getUrl());
            }
            r.add(m);
        }
        return r;
    }

    @PostMapping("/test-suites/{id}/add-requests/")
    @Transactional
    public Map<String, Object> addRequestsToSuite(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object idsObj = body.get("request_ids");
        if (!(idsObj instanceof List<?> rawList) || rawList.isEmpty()) return err("No request_ids provided");

        int added = 0;
        int maxOrder = suiteReqRepo.findByTestSuiteIdOrderByOrderNo(id).stream()
            .mapToInt(ApiTestSuiteRequest::getOrderNo).max().orElse(0);

        for (Object rid : rawList) {
            Long reqId = rid instanceof Number n ? n.longValue() : Long.parseLong(rid.toString());
            if (reqRepo.existsById(reqId) && !suiteReqRepo.existsByTestSuiteIdAndRequestId(id, reqId)) {
                ApiTestSuiteRequest sr = new ApiTestSuiteRequest();
                sr.setTestSuiteId(id); sr.setRequestId(reqId);
                sr.setOrderNo(++maxOrder); sr.setEnabled(true); sr.setAssertions("[]");
                suiteReqRepo.save(sr);
                added++;
            }
        }
        return Map.of("message", "成功添加 " + added + " 个请求");
    }

    @DeleteMapping("/test-suites/{id}/requests/{requestId}/")
    @Transactional
    public Map<String, Object> removeRequestFromSuite(@PathVariable Long id, @PathVariable Long requestId) {
        suiteReqRepo.deleteByTestSuiteIdAndRequestId(id, requestId);
        return ok();
    }

    @PatchMapping("/test-suites/{id}/requests/{requestId}/")
    @Transactional
    public Map<String, Object> updateSuiteRequest(@PathVariable Long id, @PathVariable Long requestId,
                                                   @RequestBody Map<String, Object> body) {
        List<ApiTestSuiteRequest> list = suiteReqRepo.findByTestSuiteIdOrderByOrderNo(id);
        for (ApiTestSuiteRequest sr : list) {
            if (sr.getRequestId().equals(requestId)) {
                if (body.containsKey("order")) sr.setOrderNo(intVal(body, "order", sr.getOrderNo()));
                if (body.containsKey("enabled")) sr.setEnabled(bool(body, "enabled", true));
                if (body.containsKey("assertions")) sr.setAssertions(toJsonString(body.get("assertions")));
                suiteReqRepo.save(sr);
                return ok();
            }
        }
        return err("Not found");
    }

    // ═══════════════ Suite Execution ═══════════════

    @PostMapping("/test-suites/{id}/execute/")
    @Transactional
    public Map<String, Object> executeSuite(@PathVariable Long id) {
        ApiTestSuite suite = suiteRepo.findById(id).orElse(null);
        if (suite == null) return err("Suite not found");

        ApiTestExecution exec = new ApiTestExecution();
        exec.setSuiteId(id);
        exec.setStatus("RUNNING");
        exec.setStartedAt(LocalDateTime.now());
        exec = execRepo.save(exec);

        List<ApiTestSuiteRequest> suiteReqs = suiteReqRepo.findByTestSuiteIdOrderByOrderNo(id)
            .stream().filter(sr -> sr.getEnabled() != null && sr.getEnabled()).toList();
        exec.setPassed(0); exec.setFailed(0);
        execRepo.save(exec);

        int passed = 0, failed = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        for (ApiTestSuiteRequest sr : suiteReqs) {
            ApiRequest apiReq = reqRepo.findById(sr.getRequestId()).orElse(null);
            if (apiReq == null) continue;

            try {
                Map<String, Object> execResult = doExecuteRequest(sr.getRequestId(), null);
                Object scObj = execResult.getOrDefault("status_code", 0);
                int statusCode = scObj instanceof Number n ? n.intValue() : 0;
                String respBody = (String) execResult.getOrDefault("body", "");

                boolean reqPassed = true;
                String reqError = "";
                String assertionsJson = sr.getAssertions();
                if (assertionsJson != null && !assertionsJson.isEmpty() && !"[]".equals(assertionsJson)) {
                    try {
                        JsonArray assertions = JsonParser.parseString(assertionsJson).getAsJsonArray();
                        for (JsonElement a : assertions) {
                            JsonObject assertion = a.getAsJsonObject();
                            String type = getJsonString(assertion, "type");
                            String expected = getJsonString(assertion, "expected");
                            if (expected == null) expected = getJsonString(assertion, "value"); // backward compat
                            if (type == null) continue;

                            switch (type) {
                                case "status_code" -> {
                                    int expCode = Integer.parseInt(expected != null ? expected : "200");
                                    if (statusCode != expCode) {
                                        reqPassed = false;
                                        reqError = "状态码断言失败: 期望 " + expCode + ", 实际 " + statusCode;
                                    }
                                }
                                case "response_time" -> {
                                    String rt = (String) execResult.getOrDefault("response_time", "9999ms");
                                    int ms = Integer.parseInt(rt.replaceAll("[^0-9]", ""));
                                    int expMs = Integer.parseInt(expected != null ? expected : "5000");
                                    if (ms > expMs) {
                                        reqPassed = false;
                                        reqError = "响应时间断言失败: 期望 <" + expMs + "ms, 实际 " + ms + "ms";
                                    }
                                }
                                case "contains" -> {
                                    if (expected != null && !respBody.contains(expected)) {
                                        reqPassed = false;
                                        reqError = "响应体不包含: " + expected;
                                    }
                                }
                                case "equals" -> {
                                    if (expected != null && !expected.equals(respBody)) {
                                        reqPassed = false;
                                        reqError = "响应体不等于预期值";
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logAssertionError(sr.getRequestId(), e);
                    }
                }

                if (statusCode >= 200 && statusCode < 400 && reqPassed) {
                    passed++;
                } else {
                    failed++;
                    if (reqError.isEmpty() && (statusCode < 200 || statusCode >= 400)) {
                        reqError = "HTTP " + statusCode;
                    }
                }

                Map<String, Object> r = new LinkedHashMap<>();
                r.put("request_id", sr.getRequestId());
                r.put("name", apiReq.getName()); r.put("method", apiReq.getMethod());
                r.put("url", apiReq.getUrl());
                r.put("status_code", statusCode);
                r.put("response_time", execResult.getOrDefault("response_time", "0ms"));
                r.put("passed", reqPassed);
                if (!reqError.isEmpty()) r.put("error", reqError);
                results.add(r);
            } catch (Exception e) {
                failed++;
                results.add(Map.of("request_id", sr.getRequestId(), "name", apiReq.getName(),
                    "method", apiReq.getMethod(), "url", apiReq.getUrl(),
                    "passed", false, "error", e.getMessage()));
            }
        }

        long duration = java.time.Duration.between(exec.getStartedAt(), LocalDateTime.now()).toMillis();
        exec.setStatus(failed == 0 ? "COMPLETED" : "FAILED");
        exec.setDuration(duration + "ms");
        exec.setPassed(passed);
        exec.setFailed(failed);
        exec.setFinishedAt(LocalDateTime.now());
        try { exec.setResults(new Gson().toJson(results)); } catch (Exception e) {
            log.warn("Failed to serialize suite results: {}", e.getMessage());
        }
        execRepo.save(exec);

        Map<String, Object> resp = execToMap(exec);
        resp.put("results", results);
        return resp;
    }

    // ═══════════════════ Test Executions ═══════════════════
    @GetMapping("/test-executions/")
    public Map<String, Object> listExecutions() {
        List<ApiTestExecution> list = execRepo.findAllByOrderByExecutedAtDesc();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiTestExecution e : list) {
            results.add(execToMap(e));
        }
        return paginated(results);
    }

    @GetMapping("/executions/{id}/")
    public Map<String, Object> getExecution(@PathVariable Long id) {
        ApiTestExecution e = execRepo.findById(id).orElse(null);
        if (e == null) return err("Not found");
        return execToMap(e);
    }

    @PostMapping("/test-executions/{id}/generate-allure-report/")
    public Map<String, Object> generateAllureReport(@PathVariable Long id) {
        ApiTestExecution e = execRepo.findById(id).orElse(null);
        if (e == null) return err("Not found");
        return Map.of("message", "Allure report generation started", "report_url", "/reports/" + id);
    }

    // ═══════════════════ Scheduled Tasks ═══════════════════
    @GetMapping("/scheduled-tasks/")
    public Map<String, Object> listTasks() {
        List<ApiScheduledTask> list = taskRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiScheduledTask t : list) {
            results.add(taskToMap(t));
        }
        return paginated(results);
    }

    @GetMapping("/scheduled-tasks/{id}/")
    public Map<String, Object> getTask(@PathVariable Long id) {
        ApiScheduledTask t = taskRepo.findById(id).orElse(null);
        if (t == null) return err("Not found");
        return taskToMap(t);
    }

    @PostMapping("/scheduled-tasks/")
    @Transactional
    public Map<String, Object> createTask(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        ApiScheduledTask t = new ApiScheduledTask();
        applyTaskFields(t, body);
        t = taskRepo.save(t);
        Map<String, Object> r = taskToMap(t);
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/scheduled-tasks/{id}/")
    @Transactional
    public Map<String, Object> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ApiScheduledTask t = taskRepo.findById(id).orElse(null);
        if (t == null) return err("Not found");
        applyTaskFields(t, body);
        t.setUpdatedAt(LocalDateTime.now());
        t = taskRepo.save(t);
        Map<String, Object> r = taskToMap(t);
        r.put("message", "success");
        return r;
    }

    @DeleteMapping("/scheduled-tasks/{id}/")
    @Transactional
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        taskRepo.deleteById(id);
        return ok();
    }

    @PostMapping("/scheduled-tasks/{id}/run_now/")
    @Transactional
    public Map<String, Object> runTaskNow(@PathVariable Long id) {
        ApiScheduledTask t = taskRepo.findById(id).orElse(null);
        if (t == null) return err("Not found");
        t.setLastRunAt(LocalDateTime.now());
        taskRepo.save(t);
        return Map.of("status", "started", "message", "Task queued for execution");
    }

    @PostMapping("/scheduled-tasks/{id}/pause/")
    @Transactional
    public Map<String, Object> pauseTask(@PathVariable Long id) {
        ApiScheduledTask t = taskRepo.findById(id).orElse(null);
        if (t == null) return err("Not found");
        t.setStatus("paused");
        t.setUpdatedAt(LocalDateTime.now());
        taskRepo.save(t);
        return Map.of("message", "success", "status", "paused");
    }

    @PostMapping("/scheduled-tasks/{id}/activate/")
    @Transactional
    public Map<String, Object> activateTask(@PathVariable Long id) {
        ApiScheduledTask t = taskRepo.findById(id).orElse(null);
        if (t == null) return err("Not found");
        t.setStatus("active");
        t.setUpdatedAt(LocalDateTime.now());
        taskRepo.save(t);
        return Map.of("message", "success", "status", "active");
    }

    @GetMapping("/scheduled-tasks/{id}/execution_logs/")
    public Map<String, Object> taskExecutionLogs(@PathVariable Long id) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", 1);
        log.put("task_id", id);
        log.put("start_time", LocalDateTime.now().minusMinutes(5).format(DT));
        log.put("end_time", LocalDateTime.now().format(DT));
        log.put("status", "success");
        log.put("error_message", "");
        return paginated(List.of(log));
    }

    // ═══════════════════ Operation Logs ═══════════════════
    @GetMapping("/operation-logs/")
    public Map<String, Object> operationLogs(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int page_size,
                                              @RequestParam(defaultValue = "-created_at") String ordering) {
        List<Map<String, Object>> logs = new ArrayList<>();
        List<ApiRequestHistory> recent = histRepo.findTop20ByOrderByCreatedAtDesc();
        for (ApiRequestHistory h : recent) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("operation_type", "execute");
            m.put("description", h.getMethod() + " " + h.getUrl());
            m.put("user_name", "admin");
            m.put("created_at", h.getCreatedAt() != null ? h.getCreatedAt().format(DT) : null);
            logs.add(m);
        }
        return paginated(logs);
    }

    // ═══════════════════ Users ═══════════════════
    @GetMapping("/users/")
    public List<Map<String, String>> users() {
        return List.of(
            Map.of("id", "1", "username", "admin", "email", "admin@test.com"),
            Map.of("id", "2", "username", "tester", "email", "tester@test.com")
        );
    }

    // ═══════════════════ Notification Logs ═══════════════════
    @GetMapping("/notification-logs/")
    public Map<String, Object> notificationLogs() {
        List<NotificationConfig> configs = notifRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (NotificationConfig nc : configs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", nc.getId());
            m.put("name", nc.getName());
            m.put("type", nc.getType());
            m.put("enabled", nc.getEnabled());
            m.put("created_at", nc.getCreatedAt() != null ? nc.getCreatedAt().format(DT) : null);
            results.add(m);
        }
        return paginated(results);
    }

    // ═══════════════════ Task Notification Settings ═══════════════════
    @GetMapping("/task-notification-settings/")
    public List<Map<String, Object>> taskNotificationSettings() {
        List<NotificationConfig> list = notifRepo.findAll();
        List<Map<String, Object>> r = new ArrayList<>();
        for (NotificationConfig nc : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", nc.getId()); m.put("name", nc.getName());
            m.put("type", nc.getType()); m.put("config", nc.getConfig());
            m.put("enabled", nc.getEnabled()); m.put("task_id", nc.getTaskId());
            r.add(m);
        }
        return r;
    }

    @PostMapping("/task-notification-settings/")
    @Transactional
    public Map<String, Object> createNotificationSetting(@RequestBody Map<String, Object> body) {
        NotificationConfig nc = new NotificationConfig();
        nc.setName(str(body, "name", "New Notification"));
        nc.setType(str(body, "type", "email"));
        nc.setConfig(str(body, "config"));
        nc.setEnabled(bool(body, "enabled", true));
        nc.setTaskId(lng(body, "task_id"));
        nc = notifRepo.save(nc);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", nc.getId()); r.put("name", nc.getName());
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/task-notification-settings/{id}/")
    @Transactional
    public Map<String, Object> updateNotificationSetting(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        NotificationConfig nc = notifRepo.findById(id).orElse(null);
        if (nc == null) return err("Not found");
        if (body.containsKey("name")) nc.setName(str(body, "name"));
        if (body.containsKey("type")) nc.setType(str(body, "type"));
        if (body.containsKey("config")) nc.setConfig(str(body, "config"));
        if (body.containsKey("enabled")) nc.setEnabled(bool(body, "enabled", true));
        nc.setUpdatedAt(LocalDateTime.now());
        nc = notifRepo.save(nc);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", nc.getId()); r.put("name", nc.getName());
        r.put("message", "success");
        return r;
    }

    // ═══════════════════ AI Service Configs ═══════════════════
    @GetMapping("/ai-service-configs/")
    public Map<String, Object> aiConfigs() {
        List<AIServiceConfig> list = aiRepo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (AIServiceConfig c : list) {
            results.add(aiToMap(c));
        }
        return paginated(results);
    }

    @PostMapping("/ai-service-configs/")
    @Transactional
    public Map<String, Object> createAIConfig(@RequestBody Map<String, Object> body) {
        String name = str(body, "name");
        if (name.isEmpty()) return err("Name is required");
        AIServiceConfig c = new AIServiceConfig();
        applyAIFields(c, body);
        c = aiRepo.save(c);
        Map<String, Object> r = aiToMap(c);
        r.put("message", "success");
        return r;
    }

    @PatchMapping("/ai-service-configs/{id}/")
    @Transactional
    public Map<String, Object> updateAIConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AIServiceConfig c = aiRepo.findById(id).orElse(null);
        if (c == null) return err("Not found");
        applyAIFields(c, body);
        c.setUpdatedAt(LocalDateTime.now());
        c = aiRepo.save(c);
        Map<String, Object> r = aiToMap(c);
        r.put("message", "success");
        return r;
    }

    @DeleteMapping("/ai-service-configs/{id}/")
    @Transactional
    public Map<String, Object> deleteAIConfig(@PathVariable Long id) {
        aiRepo.deleteById(id);
        return ok();
    }

    @PostMapping("/ai-service-configs/{id}/test_connection/")
    public Map<String, Object> testAIConnection(@PathVariable Long id) {
        AIServiceConfig c = aiRepo.findById(id).orElse(null);
        if (c == null) return Map.of("success", false, "message", "Config not found");

        String baseUrl = c.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
        String apiKey = c.getApiKey();
        String model = c.getModel() != null ? c.getModel() : "gpt-3.5-turbo";

        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/models/" + model))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(java.time.Duration.ofSeconds(15))
                .GET()
                .build();
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpResponse<String> resp = client.send(req,
                java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 || resp.statusCode() == 404) {
                // 200 = model found, 404 = model not found but API key is valid
                return Map.of("success", true,
                    "message", "连接成功 — " + c.getProvider() + " " + model,
                    "status_code", resp.statusCode());
            } else if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                return Map.of("success", false,
                    "message", "认证失败 (HTTP " + resp.statusCode() + ")，请检查 API Key");
            } else {
                return Map.of("success", false,
                    "message", "请求返回 HTTP " + resp.statusCode() + "，请检查配置");
            }
        } catch (java.net.http.HttpTimeoutException e) {
            return Map.of("success", false, "message", "连接超时，请检查 Base URL 和网络");
        } catch (java.net.UnknownHostException e) {
            return Map.of("success", false, "message", "无法解析主机名，请检查 Base URL");
        } catch (Exception e) {
            return Map.of("success", false, "message", "连接失败: " + e.getMessage());
        }
    }

    // ═══════════════════ Helper methods ═══════════════════

    private Map<String, Object> ok() { return Map.of("message", "success"); }

    private Map<String, Object> err(String msg) { return Map.of("message", msg, "error", true); }

    private Map<String, Object> paginated(List<Map<String, Object>> results) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size());
        resp.put("next", null);
        resp.put("previous", null);
        resp.put("results", results);
        return resp;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private Long lng(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    private boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(v.toString());
    }

    private Map<String, Object> toMap(ApiCollection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("parent", c.getParentId()); m.put("project_id", c.getProjectId());
        m.put("sort_order", c.getSortOrder());
        m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().format(DT) : null);
        m.put("updated_at", c.getUpdatedAt() != null ? c.getUpdatedAt().format(DT) : null);
        return m;
    }

    private Map<String, Object> reqToMap(ApiRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("name", r.getName());
        m.put("method", r.getMethod()); m.put("url", r.getUrl());
        m.put("collection", r.getCollectionId());
        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().format(DT) : null);
        return m;
    }

    private Map<String, Object> reqToMapFull(ApiRequest r) {
        Map<String, Object> m = reqToMap(r);
        m.put("description", r.getDescription());
        m.put("headers", parseJson(r.getHeaders()));
        m.put("params", parseJson(r.getParams()));
        m.put("body", parseJson(r.getBody()));
        m.put("body_type", r.getBodyType());
        m.put("pre_script", r.getPreScript());
        m.put("test_script", r.getTestScript());
        m.put("assertions", parseJson(r.getAssertions()));
        m.put("sort_order", r.getSortOrder());
        m.put("updated_at", r.getUpdatedAt() != null ? r.getUpdatedAt().format(DT) : null);
        return m;
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) return "";
        try {
            return new Gson().fromJson(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return new Gson().toJson(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private void applyRequestFields(ApiRequest r, Map<String, Object> body) {
        if (body.containsKey("name")) r.setName(str(body, "name"));
        if (body.containsKey("description")) r.setDescription(str(body, "description"));
        if (body.containsKey("method")) r.setMethod(str(body, "method", "GET"));
        if (body.containsKey("url")) r.setUrl(str(body, "url"));
        if (body.containsKey("headers")) r.setHeaders(toJsonString(body.get("headers")));
        if (body.containsKey("params")) r.setParams(toJsonString(body.get("params")));
        if (body.containsKey("body")) r.setBody(toJsonString(body.get("body")));
        if (body.containsKey("body_type")) r.setBodyType(str(body, "body_type", "none"));
        if (body.containsKey("pre_request_script")) r.setPreScript(str(body, "pre_request_script"));
        else if (body.containsKey("pre_script")) r.setPreScript(str(body, "pre_script"));
        if (body.containsKey("post_request_script")) r.setTestScript(str(body, "post_request_script"));
        else if (body.containsKey("test_script")) r.setTestScript(str(body, "test_script"));
        if (body.containsKey("assertions")) r.setAssertions(toJsonString(body.get("assertions")));
        if (body.containsKey("collection")) r.setCollectionId(lng(body, "collection"));
        if (body.containsKey("collection_id")) r.setCollectionId(lng(body, "collection_id"));
        if (body.containsKey("collectionId")) r.setCollectionId(lng(body, "collectionId"));
        if (body.containsKey("sort_order")) r.setSortOrder(intVal(body, "sort_order", 0));
    }

    private Map<String, Object> envToMap(ApiEnvironment e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("name", e.getName());
        m.put("description", e.getDescription());
        m.put("scope", e.getScope()); m.put("base_url", e.getBaseUrl());
        m.put("variables", e.getVariables()); m.put("project_id", e.getProjectId());
        m.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().format(DT) : null);
        m.put("updated_at", e.getUpdatedAt() != null ? e.getUpdatedAt().format(DT) : null);
        return m;
    }

    private void applyEnvFields(ApiEnvironment e, Map<String, Object> body) {
        if (body.containsKey("name")) e.setName(str(body, "name"));
        if (body.containsKey("description")) e.setDescription(str(body, "description"));
        if (body.containsKey("scope")) e.setScope(str(body, "scope"));
        if (body.containsKey("base_url")) e.setBaseUrl(str(body, "base_url"));
        if (body.containsKey("variables")) e.setVariables(str(body, "variables"));
        if (body.containsKey("project_id")) e.setProjectId(lng(body, "project_id"));
    }

    private Map<String, Object> execToMap(ApiTestExecution e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("suite_id", e.getSuiteId());
        m.put("test_suite_name", e.getSuiteId() != null ? suiteRepo.findById(e.getSuiteId())
            .map(ApiTestSuite::getName).orElse("") : "");
        m.put("status", e.getStatus()); m.put("duration", e.getDuration());
        m.put("passed", e.getPassed()); m.put("failed", e.getFailed());
        m.put("passed_requests", e.getPassed() != null ? e.getPassed() : 0);
        m.put("failed_requests", e.getFailed() != null ? e.getFailed() : 0);
        int total = (e.getPassed() != null ? e.getPassed() : 0) + (e.getFailed() != null ? e.getFailed() : 0);
        m.put("total_requests", total);
        // Parse results JSON string to array for frontend
        String resultsStr = e.getResults();
        if (resultsStr != null && !resultsStr.isEmpty()) {
            try { m.put("results", new Gson().fromJson(resultsStr, Object.class)); }
            catch (Exception ex) { m.put("results", resultsStr); }
        } else {
            m.put("results", new ArrayList<>());
        }
        m.put("created_at", e.getStartedAt() != null ? e.getStartedAt().format(DT) : null);
        m.put("executed_at", e.getExecutedAt() != null ? e.getExecutedAt().format(DT) : null);
        // Placeholder for frontend compatibility
        Map<String, String> executor = new LinkedHashMap<>();
        executor.put("username", "admin");
        m.put("executed_by", executor);
        return m;
    }

    private Map<String, Object> taskToMap(ApiScheduledTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId()); m.put("name", t.getName());
        m.put("description", t.getDescription());
        m.put("task_type", t.getTaskType());
        m.put("trigger_type", t.getTriggerType());
        m.put("cron_expression", t.getCronExpression());
        m.put("request_ids", t.getRequestIds());
        m.put("test_suite", t.getTestSuiteId());
        m.put("environment", t.getEnvironmentId());
        m.put("interval_seconds", t.getIntervalSeconds());
        m.put("execute_at", t.getExecuteAt() != null ? t.getExecuteAt().format(DT) : null);
        m.put("notify_on_success", t.getNotifyOnSuccess());
        m.put("notify_on_failure", t.getNotifyOnFailure());
        m.put("notification_type", t.getNotificationType());
        m.put("notification_type_display", t.getNotificationType());
        m.put("notify_emails", t.getNotifyEmails());
        m.put("status", t.getStatus());
        m.put("last_run_time", t.getLastRunAt() != null ? t.getLastRunAt().format(DT) : null);
        m.put("last_run_at", t.getLastRunAt() != null ? t.getLastRunAt().format(DT) : null);
        m.put("next_run_time", t.getNextRunAt() != null ? t.getNextRunAt().format(DT) : null);
        m.put("created_at", t.getCreatedAt() != null ? t.getCreatedAt().format(DT) : null);
        return m;
    }

    private void applyTaskFields(ApiScheduledTask t, Map<String, Object> body) {
        if (body.containsKey("name")) t.setName(str(body, "name"));
        if (body.containsKey("description")) t.setDescription(str(body, "description"));
        if (body.containsKey("task_type")) t.setTaskType(str(body, "task_type"));
        if (body.containsKey("trigger_type")) t.setTriggerType(str(body, "trigger_type", "cron"));
        if (body.containsKey("cron_expression")) t.setCronExpression(str(body, "cron_expression"));
        if (body.containsKey("request_ids")) t.setRequestIds(str(body, "request_ids"));
        if (body.containsKey("test_suite")) t.setTestSuiteId(lng(body, "test_suite"));
        if (body.containsKey("environment")) t.setEnvironmentId(lng(body, "environment"));
        if (body.containsKey("interval_seconds")) t.setIntervalSeconds(intVal(body, "interval_seconds", 0));
        if (body.containsKey("execute_at")) {
            Object v = body.get("execute_at");
            if (v != null && !v.toString().isEmpty()) {
                try { t.setExecuteAt(LocalDateTime.parse(v.toString(), DT)); }
                catch (Exception e) { try { t.setExecuteAt(LocalDateTime.parse(v.toString())); } catch (Exception ignored) {} }
            }
        }
        if (body.containsKey("notify_on_success")) t.setNotifyOnSuccess(bool(body, "notify_on_success", false));
        if (body.containsKey("notify_on_failure")) t.setNotifyOnFailure(bool(body, "notify_on_failure", false));
        if (body.containsKey("notification_type")) t.setNotificationType(str(body, "notification_type"));
        if (body.containsKey("notification_type_input")) t.setNotificationType(str(body, "notification_type_input"));
        if (body.containsKey("notify_emails")) t.setNotifyEmails(str(body, "notify_emails"));
        if (body.containsKey("status")) t.setStatus(str(body, "status", "active"));
    }

    private Map<String, Object> aiToMap(AIServiceConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName());
        m.put("provider", c.getProvider());
        m.put("service_type", c.getProvider());        // frontend alias
        m.put("service_type_display", c.getProvider()); // frontend display
        m.put("model", c.getModel());
        m.put("model_name", c.getModel());              // frontend alias
        // Mask API key - only show last 4 characters
        String key = c.getApiKey();
        if (key != null && key.length() > 4) {
            m.put("api_key", "****" + key.substring(key.length() - 4));
        } else {
            m.put("api_key", key);
        }
        m.put("base_url", c.getBaseUrl());
        m.put("config", c.getConfig());
        m.put("status", c.getStatus());
        m.put("is_active", "active".equals(c.getStatus()));  // frontend expects boolean
        m.put("role", c.getRole());
        m.put("role_display", c.getRole());
        m.put("max_tokens", c.getMaxTokens());
        m.put("temperature", c.getTemperature());
        m.put("created_by_name", c.getCreatedByName());
        m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().format(DT) : null);
        return m;
    }

    private void applyAIFields(AIServiceConfig c, Map<String, Object> body) {
        if (body.containsKey("name")) c.setName(str(body, "name"));
        if (body.containsKey("provider")) c.setProvider(str(body, "provider"));
        if (body.containsKey("service_type")) c.setProvider(str(body, "service_type"));
        if (body.containsKey("api_key") && !str(body, "api_key").startsWith("****")) {
            c.setApiKey(str(body, "api_key"));
        }
        if (body.containsKey("model")) c.setModel(str(body, "model"));
        if (body.containsKey("model_name")) c.setModel(str(body, "model_name"));
        if (body.containsKey("base_url")) c.setBaseUrl(str(body, "base_url"));
        if (body.containsKey("config")) c.setConfig(str(body, "config"));
        if (body.containsKey("status")) c.setStatus(str(body, "status", "active"));
        if (body.containsKey("is_active")) {
            c.setStatus(bool(body, "is_active", true) ? "active" : "inactive");
        }
        if (body.containsKey("role")) c.setRole(str(body, "role"));
        if (body.containsKey("max_tokens")) c.setMaxTokens(intVal(body, "max_tokens", 4096));
        if (body.containsKey("temperature")) {
            Object tv = body.get("temperature");
            if (tv instanceof Number n) c.setTemperature(n.doubleValue());
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiTestingController.class);

    private String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement e = obj.get(key);
        return e.isJsonNull() ? null : e.getAsString();
    }

    private void logAssertionError(Long requestId, Exception e) {
        log.warn("Assertion parse failed for suite request {}: {}", requestId, e.getMessage());
    }
}
