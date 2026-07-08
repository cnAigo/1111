package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_requests")
public class ApiRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2000)
    private String description;
    private String method = "GET";
    @Column(length = 2000)
    private String url;
    @Column(columnDefinition = "TEXT")
    private String headers;
    @Column(columnDefinition = "TEXT")
    private String params;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String body;
    private String bodyType = "none";
    @Column(columnDefinition = "TEXT")
    private String preScript;
    @Column(columnDefinition = "TEXT")
    private String testScript;
    @Column(columnDefinition = "TEXT")
    private String assertions;
    private Long collectionId;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ApiRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public String getMethod() { return method; }
    public void setMethod(String m) { this.method = m; }
    public String getUrl() { return url; }
    public void setUrl(String u) { this.url = u; }
    public String getHeaders() { return headers; }
    public void setHeaders(String h) { this.headers = h; }
    public String getParams() { return params; }
    public void setParams(String p) { this.params = p; }
    public String getBody() { return body; }
    public void setBody(String b) { this.body = b; }
    public String getBodyType() { return bodyType; }
    public void setBodyType(String bt) { this.bodyType = bt; }
    public String getPreScript() { return preScript; }
    public void setPreScript(String s) { this.preScript = s; }
    public String getTestScript() { return testScript; }
    public void setTestScript(String s) { this.testScript = s; }
    public String getAssertions() { return assertions; }
    public void setAssertions(String a) { this.assertions = a; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long cid) { this.collectionId = cid; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer o) { this.sortOrder = o; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
