package org.example.testvue.controller;

import org.example.testvue.entity.ApiProject;
import org.example.testvue.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/api-testing")
public class ApiProjectController {

    private final ApiProjectRepository repo;
    private final ApiCollectionRepository collRepo;
    private final ApiRequestRepository reqRepo;
    private final ApiEnvironmentRepository envRepo;
    private final ApiTestSuiteRepository suiteRepo;
    private final ApiTestSuiteRequestRepository suiteReqRepo;
    private final ApiTestExecutionRepository execRepo;
    private final ApiRequestHistoryRepository histRepo;

    public ApiProjectController(ApiProjectRepository repo, ApiCollectionRepository collRepo,
                                ApiRequestRepository reqRepo, ApiEnvironmentRepository envRepo,
                                ApiTestSuiteRepository suiteRepo, ApiTestSuiteRequestRepository suiteReqRepo,
                                ApiTestExecutionRepository execRepo, ApiRequestHistoryRepository histRepo) {
        this.repo = repo;
        this.collRepo = collRepo;
        this.reqRepo = reqRepo;
        this.envRepo = envRepo;
        this.suiteRepo = suiteRepo;
        this.suiteReqRepo = suiteReqRepo;
        this.execRepo = execRepo;
        this.histRepo = histRepo;
    }

    @GetMapping("/projects/")
    public Map<String, Object> list() {
        List<ApiProject> all = repo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ApiProject p : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("description", p.getDescription());
            m.put("project_type", p.getProjectType());
            m.put("status", p.getStatus());
            m.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
            m.put("updated_at", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
            results.add(m);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size());
        resp.put("results", results);
        return resp;
    }

    @GetMapping("/projects/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId()); m.put("name", p.getName());
            m.put("description", p.getDescription()); m.put("project_type", p.getProjectType());
            m.put("status", p.getStatus()); m.put("created_at", p.getCreatedAt().toString());
            m.put("updated_at", p.getUpdatedAt().toString());
            return m;
        }).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/projects/")
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        ApiProject p = new ApiProject();
        p.setName((String) body.getOrDefault("name", "New Project"));
        p.setDescription((String) body.getOrDefault("description", ""));
        p.setProjectType((String) body.getOrDefault("project_type", "HTTP"));
        p.setStatus((String) body.getOrDefault("status", "active"));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p = repo.save(p);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", p.getId()); r.put("name", p.getName()); r.put("message", "success");
        return r;
    }

    @PatchMapping("/projects/{id}/")
    @Transactional
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            if (body.containsKey("name")) p.setName((String) body.get("name"));
            if (body.containsKey("description")) p.setDescription((String) body.get("description"));
            if (body.containsKey("project_type")) p.setProjectType((String) body.get("project_type"));
            if (body.containsKey("status")) p.setStatus((String) body.get("status"));
            p.setUpdatedAt(LocalDateTime.now());
            repo.save(p);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", p.getId()); r.put("name", p.getName()); r.put("message", "success");
            return r;
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/projects/{id}/")
    @Transactional
    public Map<String, Object> delete(@PathVariable Long id) {
        // Cascade delete all related data
        // Delete execution records & suite-request associations for project's suites
        List<org.example.testvue.entity.ApiTestSuite> suites = suiteRepo.findByProjectId(id);
        for (org.example.testvue.entity.ApiTestSuite suite : suites) {
            suiteReqRepo.deleteByTestSuiteId(suite.getId());
            execRepo.deleteBySuiteId(suite.getId());
        }
        suiteRepo.deleteByProjectId(id);
        envRepo.deleteByProjectId(id);
        // Delete requests in project's collections
        List<org.example.testvue.entity.ApiCollection> colls = collRepo.findByProjectIdOrderBySortOrder(id);
        for (org.example.testvue.entity.ApiCollection coll : colls) {
            reqRepo.deleteByCollectionId(coll.getId());
        }
        collRepo.deleteByProjectId(id);
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }
}
