package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_scheduled_tasks")
public class ApiScheduledTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 1000)
    private String description;
    private String taskType;
    private String triggerType = "cron";
    private String cronExpression;
    @Column(columnDefinition = "TEXT")
    private String requestIds;
    private Long testSuiteId;
    private Long environmentId;
    private Integer intervalSeconds;
    private LocalDateTime executeAt;
    private Boolean notifyOnSuccess = false;
    private Boolean notifyOnFailure = false;
    private String notificationType;
    private String notifyEmails;
    private String status = "active";
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ApiScheduledTask() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String t) { this.taskType = t; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String t) { this.triggerType = t; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String c) { this.cronExpression = c; }
    public String getRequestIds() { return requestIds; }
    public void setRequestIds(String ids) { this.requestIds = ids; }
    public Long getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(Long id) { this.testSuiteId = id; }
    public Long getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(Long id) { this.environmentId = id; }
    public Integer getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Integer s) { this.intervalSeconds = s; }
    public LocalDateTime getExecuteAt() { return executeAt; }
    public void setExecuteAt(LocalDateTime t) { this.executeAt = t; }
    public Boolean getNotifyOnSuccess() { return notifyOnSuccess; }
    public void setNotifyOnSuccess(Boolean b) { this.notifyOnSuccess = b; }
    public Boolean getNotifyOnFailure() { return notifyOnFailure; }
    public void setNotifyOnFailure(Boolean b) { this.notifyOnFailure = b; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String t) { this.notificationType = t; }
    public String getNotifyEmails() { return notifyEmails; }
    public void setNotifyEmails(String e) { this.notifyEmails = e; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime t) { this.lastRunAt = t; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime t) { this.nextRunAt = t; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
