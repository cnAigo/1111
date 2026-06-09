package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_history")
public class TestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String taskId;

    @Column(length = 256)
    private String label;

    @Column(length = 32)
    private String status;

    @Column(length = 32)
    private String durationFmt;

    private int passed;
    private int failed;
    private int skipped;

    /** Real-time progress — updated during execution */
    private int progress;
    private int progressTotal;
    private long durationMs;

    /** Final error message (if any) */
    @Column(length = 512)
    private String errorMessage;

    /** Path to the on-disk log file */
    @Column(length = 512)
    private String logFilePath;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public TestHistory() {}

    /** Named factory — no risk of parameter ordering errors. */
    public static TestHistory of(String taskId, String label, String status, String durationFmt,
                                  int passed, int failed, int skipped, String output, String resultJson) {
        TestHistory h = new TestHistory();
        h.taskId = taskId; h.label = label; h.status = status; h.durationFmt = durationFmt;
        h.passed = passed; h.failed = failed; h.skipped = skipped;
        h.output = output; h.resultJson = resultJson;
        return h;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDurationFmt() { return durationFmt; }
    public void setDurationFmt(String durationFmt) { this.durationFmt = durationFmt; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getProgressTotal() { return progressTotal; }
    public void setProgressTotal(int progressTotal) { this.progressTotal = progressTotal; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
