package org.example.testvue.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_class_stats")
public class TestClassStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, unique = true, nullable = false)
    private String className;

    /** Number of test methods in this class (from Surefire results). */
    private int methodCount;

    /** Average execution duration in milliseconds. */
    private long avgDurationMs;

    /** Number of samples used to compute avgDurationMs. */
    private int sampleCount;

    public TestClassStats() {}

    public TestClassStats(String className, int methodCount, long avgDurationMs, int sampleCount) {
        this.className = className;
        this.methodCount = methodCount;
        this.avgDurationMs = avgDurationMs;
        this.sampleCount = sampleCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public int getMethodCount() { return methodCount; }
    public void setMethodCount(int methodCount) { this.methodCount = methodCount; }
    public long getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(long avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
}
