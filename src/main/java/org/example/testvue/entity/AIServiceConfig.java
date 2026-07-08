package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_service_configs")
public class AIServiceConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String provider;
    @Column(length = 500)
    private String apiKey;
    private String model;
    @Column(length = 1000)
    private String baseUrl;
    @Column(columnDefinition = "TEXT")
    private String config;
    private String role;
    private Integer maxTokens = 4096;
    private Double temperature = 0.7;
    private String status = "active";
    private String createdByName = "admin";
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AIServiceConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvider() { return provider; }
    public void setProvider(String p) { this.provider = p; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String k) { this.apiKey = k; }
    public String getModel() { return model; }
    public void setModel(String m) { this.model = m; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String u) { this.baseUrl = u; }
    public String getConfig() { return config; }
    public void setConfig(String c) { this.config = c; }
    public String getRole() { return role; }
    public void setRole(String r) { this.role = r; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer mt) { this.maxTokens = mt; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double t) { this.temperature = t; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String n) { this.createdByName = n; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
