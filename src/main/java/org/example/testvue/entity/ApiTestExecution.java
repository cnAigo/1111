package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_test_executions")
public class ApiTestExecution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long suiteId;
    private String status = "pending";
    private String duration;
    private Integer passed = 0;
    private Integer failed = 0;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String results;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime executedAt = LocalDateTime.now();

    public ApiTestExecution() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSuiteId() { return suiteId; }
    public void setSuiteId(Long sid) { this.suiteId = sid; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getDuration() { return duration; }
    public void setDuration(String d) { this.duration = d; }
    public Integer getPassed() { return passed; }
    public void setPassed(Integer p) { this.passed = p; }
    public Integer getFailed() { return failed; }
    public void setFailed(Integer f) { this.failed = f; }
    public String getResults() { return results; }
    public void setResults(String r) { this.results = r; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime t) { this.startedAt = t; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime t) { this.finishedAt = t; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime t) { this.executedAt = t; }
}
