package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_test_environments")
public class UiTestEnvironment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2000)
    private String description;
    private String browser = "chromium";
    private String baseUrl;
    @Column(columnDefinition = "TEXT")
    private String variables; // JSON
    private Long projectId;
    private Boolean isActive = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getBrowser() { return browser; } public void setBrowser(String b) { this.browser = b; }
    public String getBaseUrl() { return baseUrl; } public void setBaseUrl(String u) { this.baseUrl = u; }
    public String getVariables() { return variables; } public void setVariables(String v) { this.variables = v; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long p) { this.projectId = p; }
    public Boolean getIsActive() { return isActive; } public void setIsActive(Boolean a) { this.isActive = a; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
