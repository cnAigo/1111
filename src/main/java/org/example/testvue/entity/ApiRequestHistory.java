package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_request_history")
public class ApiRequestHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long requestId;
    private String requestName;
    private String method;
    @Column(length = 2000)
    private String url;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestHeaders;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestParams;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String requestBody;
    private Integer statusCode;
    private String responseTime;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String responseHeaders;
    @Column(columnDefinition = "LONGTEXT")
    private String responseBody;
    private LocalDateTime createdAt = LocalDateTime.now();

    public ApiRequestHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long rid) { this.requestId = rid; }
    public String getRequestName() { return requestName; }
    public void setRequestName(String n) { this.requestName = n; }
    public String getMethod() { return method; }
    public void setMethod(String m) { this.method = m; }
    public String getUrl() { return url; }
    public void setUrl(String u) { this.url = u; }
    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String h) { this.requestHeaders = h; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String p) { this.requestParams = p; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String b) { this.requestBody = b; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer c) { this.statusCode = c; }
    public String getResponseTime() { return responseTime; }
    public void setResponseTime(String t) { this.responseTime = t; }
    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String h) { this.responseHeaders = h; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String b) { this.responseBody = b; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
