package org.example.testvue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.testvue.entity.AiModelConfig;
import org.example.testvue.repository.AiModelConfigRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class MiMoService {

    private final AiModelConfigRepository configRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public MiMoService(AiModelConfigRepository configRepo) { this.configRepo = configRepo; }

    public String callAi(String taskDescription) {
        return callAi("你是一个UI自动化测试专家。根据用户的任务描述，分析并生成测试步骤。返回JSON格式：{\"steps\":[{\"action\":\"操作\",\"target\":\"目标元素\"}]}", taskDescription);
    }

    public String callAi(String systemPrompt, String userMessage) {
        AiModelConfig cfg = configRepo.findByIsActiveTrue().orElse(null);
        if (cfg == null) return "错误：未启用AI配置，请先在API配置中启用一个模型";

        String baseUrl = (cfg.getBaseUrl() != null && !cfg.getBaseUrl().isBlank())
            ? cfg.getBaseUrl().replaceAll("/+$", "") : "https://api.xiaomimimo.com/v1";
        String model = cfg.getModelName() != null ? cfg.getModelName() : "mimo-v2.5";
        String apiKey = cfg.getApiKey();

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ));
            body.put("max_tokens", 1000);

            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String rawBody = resp.body();
            System.err.println("MIMO RAW: " + (rawBody != null ? rawBody.substring(0, Math.min(500, rawBody.length())) : "null"));
            if (resp.statusCode() == 200) {
                try {
                    Map<String, Object> result = mapper.readValue(rawBody, Map.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                        if (msg != null) {
                            String content = (String) msg.get("content");
                            if (content != null && !content.isBlank()) return content;
                            // MiMo puts content in reasoning_content field
                            String reasoning = (String) msg.get("reasoning_content");
                            if (reasoning != null && !reasoning.isBlank()) return reasoning;
                        }
                    }
                } catch (Exception parseErr) {
                    System.err.println("MIMO PARSE: " + parseErr.getMessage());
                }
                return rawBody;
            }
            return "API返回 " + resp.statusCode() + ": " + rawBody.substring(0, Math.min(300, rawBody.length()));
        } catch (Exception e) {
            return "调用失败: " + e.getMessage();
        }
    }
}
