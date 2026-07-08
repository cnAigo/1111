package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_test_executions")
public class UiTestExecution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long testCaseId;
    private String testCaseName;
    private String status = "pending";
    private String engine = "playwright";
    private String browser = "chromium";
    private Integer passed = 0;
    private Integer failed = 0;
    private Integer total = 0;
    private String duration;
    @Column(columnDefinition = "TEXT")
    private String logs;
    @Column(columnDefinition = "TEXT")
    private String screenshots; // JSON array of file paths
    private Long projectId;
    private LocalDateTime executedAt = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();

    public UiTestExecution() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getTestCaseId() { return testCaseId; } public void setTestCaseId(Long id) { this.testCaseId = id; }
    public String getTestCaseName() { return testCaseName; } public void setTestCaseName(String n) { this.testCaseName = n; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getEngine() { return engine; } public void setEngine(String e) { this.engine = e; }
    public String getBrowser() { return browser; } public void setBrowser(String b) { this.browser = b; }
    public Integer getPassed() { return passed; } public void setPassed(Integer p) { this.passed = p; }
    public Integer getFailed() { return failed; } public void setFailed(Integer f) { this.failed = f; }
    public Integer getTotal() { return total; } public void setTotal(Integer t) { this.total = t; }
    public String getDuration() { return duration; } public void setDuration(String d) { this.duration = d; }
    public String getLogs() { return logs; } public void setLogs(String l) { this.logs = l; }
    public String getScreenshots() { return screenshots; } public void setScreenshots(String s) { this.screenshots = s; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long p) { this.projectId = p; }
    public LocalDateTime getExecutedAt() { return executedAt; } public void setExecutedAt(LocalDateTime t) { this.executedAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
