package org.example.testvue.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api_test_suite_requests", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"test_suite_id", "request_id"})
})
public class ApiTestSuiteRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_suite_id")
    private Long testSuiteId;

    @Column(name = "request_id")
    private Long requestId;

    private Integer orderNo = 0;

    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String assertions;

    public ApiTestSuiteRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(Long sid) { this.testSuiteId = sid; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long rid) { this.requestId = rid; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer o) { this.orderNo = o; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean e) { this.enabled = e; }
    public String getAssertions() { return assertions; }
    public void setAssertions(String a) { this.assertions = a; }
}
