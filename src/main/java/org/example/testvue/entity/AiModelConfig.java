package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_configs")
public class AiModelConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String modelType = "openai";
    private String modelName;
    private String apiKey;
    private String baseUrl;
    private Boolean isActive = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getModelType() { return modelType; } public void setModelType(String t) { this.modelType = t; }
    public String getModelName() { return modelName; } public void setModelName(String n) { this.modelName = n; }
    public String getApiKey() { return apiKey; } public void setApiKey(String k) { this.apiKey = k; }
    public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String u) { this.baseUrl = u; }
    public Boolean getIsActive() { return isActive; } public void setIsActive(Boolean a) { this.isActive = a; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
