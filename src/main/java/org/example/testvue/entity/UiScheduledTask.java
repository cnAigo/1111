package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_scheduled_tasks")
public class UiScheduledTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2000)
    private String description;
    private String taskType;  // TEST_SUITE / TEST_CASE
    private String triggerType; // CRON / INTERVAL / ONCE
    private String cronExpression;
    private String engine = "playwright";
    private String status = "active"; // active / paused
    private Long testSuiteId;
    private Long testCaseId;
    private LocalDateTime lastRunAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getTaskType() { return taskType; } public void setTaskType(String t) { this.taskType = t; }
    public String getTriggerType() { return triggerType; } public void setTriggerType(String t) { this.triggerType = t; }
    public String getCronExpression() { return cronExpression; } public void setCronExpression(String c) { this.cronExpression = c; }
    public String getEngine() { return engine; } public void setEngine(String e) { this.engine = e; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public Long getTestSuiteId() { return testSuiteId; } public void setTestSuiteId(Long id) { this.testSuiteId = id; }
    public Long getTestCaseId() { return testCaseId; } public void setTestCaseId(Long id) { this.testCaseId = id; }
    public LocalDateTime getLastRunAt() { return lastRunAt; } public void setLastRunAt(LocalDateTime t) { this.lastRunAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
