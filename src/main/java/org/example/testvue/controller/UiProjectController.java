package org.example.testvue.controller;

import org.example.testvue.entity.UiProject;
import org.example.testvue.repository.UiProjectRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation")
public class UiProjectController {

    private final UiProjectRepository repo;

    public UiProjectController(UiProjectRepository repo) { this.repo = repo; }

    @GetMapping("/projects/")
    public Map<String, Object> list() {
        List<UiProject> all = repo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiProject p : all) results.add(toMap(p));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size());
        resp.put("results", results);
        return resp;
    }

    @GetMapping("/projects/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @PostMapping("/projects/")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        UiProject p = new UiProject();
        updateFromBody(p, body);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p = repo.save(p);
        return toMap(p);
    }

    @PatchMapping("/projects/{id}/")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            updateFromBody(p, body);
            p.setUpdatedAt(LocalDateTime.now());
            repo.save(p);
            return toMap(p);
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/projects/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    // ── Helper ──
    private Map<String, Object> toMap(UiProject p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName());
        m.put("description", p.getDescription()); m.put("base_url", p.getBaseUrl());
        m.put("status", p.getStatus());
        m.put("owner", Map.of("id", p.getOwnerId() != null ? p.getOwnerId() : 0, "username", p.getOwnerName() != null ? p.getOwnerName() : ""));
        m.put("start_date", p.getStartDate()); m.put("end_date", p.getEndDate());
        m.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        m.put("updated_at", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null);
        return m;
    }

    private void updateFromBody(UiProject p, Map<String, Object> body) {
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("description")) p.setDescription((String) body.get("description"));
        if (body.containsKey("base_url")) p.setBaseUrl((String) body.get("base_url"));
        if (body.containsKey("status")) p.setStatus((String) body.get("status"));
        if (body.containsKey("start_date")) p.setStartDate((String) body.get("start_date"));
        if (body.containsKey("end_date")) p.setEndDate((String) body.get("end_date"));
        if (body.containsKey("owner_id")) {
            Object oid = body.get("owner_id");
            if (oid instanceof Number) p.setOwnerId(((Number) oid).longValue());
            else if (oid != null) p.setOwnerId(Long.valueOf(oid.toString()));
        }
    }
}
