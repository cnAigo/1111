package org.example.testvue.controller;

import org.example.testvue.entity.AiModelConfig;
import org.example.testvue.repository.AiModelConfigRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation/config")
public class AiModelConfigController {

    private final AiModelConfigRepository repo;

    public AiModelConfigController(AiModelConfigRepository repo) { this.repo = repo; }

    @GetMapping("/ai-mode/")
    public List<Map<String, Object>> list() {
        List<AiModelConfig> all = repo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiModelConfig c : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("name", c.getName());
            m.put("model_type", c.getModelType()); m.put("model_name", c.getModelName());
            m.put("base_url", c.getBaseUrl()); m.put("is_active", c.getIsActive());
            m.put("api_key_length", c.getApiKey() != null ? c.getApiKey().length() : 0);
            m.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            result.add(m);
        }
        return result;
    }

    @PostMapping("/ai-mode/")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        AiModelConfig c = new AiModelConfig();
        updateFields(c, body);
        if (Boolean.TRUE.equals(c.getIsActive())) {
            repo.findByIsActiveTrue().ifPresent(old -> { old.setIsActive(false); repo.save(old); });
        }
        c.setCreatedAt(LocalDateTime.now()); c.setUpdatedAt(LocalDateTime.now());
        c = repo.save(c);
        return Map.of("id", c.getId(), "name", c.getName(), "message", "success");
    }

    @PatchMapping("/ai-mode/{id}/")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(c -> {
            updateFields(c, body);
            if (Boolean.TRUE.equals(body.get("is_active"))) {
                repo.findByIsActiveTrue().ifPresent(old -> { old.setIsActive(false); repo.save(old); });
            }
            c.setUpdatedAt(LocalDateTime.now());
            repo.save(c);
            Map<String,Object> r = new LinkedHashMap<>(); r.put("id", c.getId()); r.put("message", "success"); return r;
        }).orElse(new LinkedHashMap<>() {{ put("error", "not found"); }});
    }

    @PostMapping("/ai-mode/{id}/activate/")
    public Map<String, Object> activate(@PathVariable Long id) {
        AiModelConfig c = repo.findById(id).orElse(null);
        if (c == null) return Map.of("error", "not found");
        // Test connection
        Map<String, Object> testResult = testApiKey(c.getBaseUrl(), c.getApiKey());
        if (!Boolean.TRUE.equals(testResult.get("success"))) return testResult;
        // Activate
        repo.findByIsActiveTrue().ifPresent(old -> { old.setIsActive(false); repo.save(old); });
        c.setIsActive(true); repo.save(c);
        return Map.of("success", true, "message", "连接成功，已启用");
    }

    @DeleteMapping("/ai-mode/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    @PostMapping("/ai-mode/test_connection/")
    public Map<String, Object> testConnection(@RequestBody Map<String, Object> body) {
        String apiKey = (String) body.get("api_key");
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "API Key为空");
        Object baseUrl = body.get("base_url");
        String base = (baseUrl != null && !baseUrl.toString().isBlank())
            ? baseUrl.toString().replaceAll("/+$", "") : "https://api.openai.com/v1";
        return testApiKey(base, apiKey);
    }

    private Map<String, Object> testApiKey(String baseUrl, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return Map.of("success", false, "error", "API Key为空");
        try {
            String base = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
            String url = base + "/models";
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(java.time.Duration.ofSeconds(10))
                .GET().build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            Map<String,Object> r = new LinkedHashMap<>();
            r.put("success", resp.statusCode() == 200);
            r.put("status", resp.statusCode());
            r.put(resp.statusCode() == 200 ? "message" : "error",
                resp.statusCode() == 200 ? "连接成功" : "HTTP " + resp.statusCode());
            return r;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private void updateFields(AiModelConfig c, Map<String, Object> body) {
        if (body.containsKey("name")) c.setName((String) body.get("name"));
        if (body.containsKey("model_type")) c.setModelType((String) body.get("model_type"));
        if (body.containsKey("model_name")) c.setModelName((String) body.get("model_name"));
        if (body.containsKey("api_key") && body.get("api_key") != null && !body.get("api_key").toString().isBlank())
            c.setApiKey((String) body.get("api_key"));
        if (body.containsKey("base_url")) c.setBaseUrl((String) body.get("base_url"));
        if (body.containsKey("is_active")) c.setIsActive((Boolean) body.get("is_active"));
    }
}
