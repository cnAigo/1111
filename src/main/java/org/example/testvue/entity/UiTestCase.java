package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_test_cases")
public class UiTestCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2000)
    private String description;
    private String priority = "medium";
    private String status = "draft";
    @Column(columnDefinition = "TEXT")
    private String steps;       // JSON string of steps
    private Long projectId;
    private String engine = "playwright";
    private String browser = "chromium";
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UiTestCase() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getPriority() { return priority; } public void setPriority(String p) { this.priority = p; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getSteps() { return steps; } public void setSteps(String s) { this.steps = s; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long p) { this.projectId = p; }
    public String getEngine() { return engine; } public void setEngine(String e) { this.engine = e; }
    public String getBrowser() { return browser; } public void setBrowser(String b) { this.browser = b; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
