package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_execution_records")
public class AiExecutionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long caseId;
    private String caseName;
    @Column(length = 2000)
    private String taskDescription;
    private String status = "running";
    private Integer progress = 0;
    private Long duration;
    private LocalDateTime startTime;
    @Lob @Column(columnDefinition = "LONGTEXT")
    private String result;
    @Lob @Column(columnDefinition = "LONGTEXT")
    private String steps; // JSON
    @Lob @Column(columnDefinition = "LONGTEXT")
    private String screenshots; // JSON array of paths
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; } public void setCaseId(Long id) { this.caseId = id; }
    public String getCaseName() { return caseName; } public void setCaseName(String n) { this.caseName = n; }
    public String getTaskDescription() { return taskDescription; } public void setTaskDescription(String s) { this.taskDescription = s; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public Integer getProgress() { return progress; } public void setProgress(Integer p) { this.progress = p; }
    public Long getDuration() { return duration; } public void setDuration(Long d) { this.duration = d; }
    public LocalDateTime getStartTime() { return startTime; } public void setStartTime(LocalDateTime t) { this.startTime = t; }
    public String getResult() { return result; } public void setResult(String s) { this.result = s; }
    public String getSteps() { return steps; } public void setSteps(String s) { this.steps = s; }
    public String getScreenshots() { return screenshots; } public void setScreenshots(String s) { this.screenshots = s; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
