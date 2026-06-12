package org.example.testvue.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_case_detail")
public class TestCaseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)  private String caseId;       // GNYL_012, UI-072 etc
    @Column(length = 64)  private String module;       // 文件夹, 合作区 etc
    @Column(length = 256) private String title;        // 用例标题
    @Column(length = 32)  private String caseType;     // 正向/负向/API/UI
    @Column(columnDefinition = "TEXT") private String steps;     // 用例步骤
    @Column(columnDefinition = "TEXT") private String expected; // 预期结果
    @Column(length = 256) private String apiUrl;       // 接口URL (API用例)
    @Column(length = 32)  private String httpMethod;   // GET/POST
    @Column(length = 128) private String javaMethod;   // 对应的Java测试方法名
    @Column(length = 128) private String className;    // 对应的测试类名
    private Long avgDurationMs;   // 历史平均执行时长(毫秒)，用于进度估算
    private Integer sampleCount;  // 采样次数

    public TestCaseDetail() {}

    // fluent setters for builder-like usage
    public TestCaseDetail setCaseId(String v) { this.caseId = v; return this; }
    public TestCaseDetail setModule(String v) { this.module = v; return this; }
    public TestCaseDetail setTitle(String v) { this.title = v; return this; }
    public TestCaseDetail setCaseType(String v) { this.caseType = v; return this; }
    public TestCaseDetail setSteps(String v) { this.steps = v; return this; }
    public TestCaseDetail setExpected(String v) { this.expected = v; return this; }
    public TestCaseDetail setApiUrl(String v) { this.apiUrl = v; return this; }
    public TestCaseDetail setHttpMethod(String v) { this.httpMethod = v; return this; }
    public TestCaseDetail setJavaMethod(String v) { this.javaMethod = v; return this; }
    public TestCaseDetail setClassName(String v) { this.className = v; return this; }
    public TestCaseDetail setAvgDurationMs(Long v) { this.avgDurationMs = v; return this; }
    public TestCaseDetail setSampleCount(Integer v) { this.sampleCount = v; return this; }

    // getters
    public Long getId() { return id; }
    public String getCaseId() { return caseId; }
    public String getModule() { return module; }
    public String getTitle() { return title; }
    public String getCaseType() { return caseType; }
    public String getSteps() { return steps; }
    public String getExpected() { return expected; }
    public String getApiUrl() { return apiUrl; }
    public String getHttpMethod() { return httpMethod; }
    public String getJavaMethod() { return javaMethod; }
    public String getClassName() { return className; }
    public Long getAvgDurationMs() { return avgDurationMs; }
    public Integer getSampleCount() { return sampleCount; }
}
