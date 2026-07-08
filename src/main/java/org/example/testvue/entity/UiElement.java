package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_elements")
public class UiElement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 1000)
    private String description;
    private String elementType = "BUTTON";
    private String locatorStrategy = "CSS Selector";
    private String locatorValue;
    private String page;           // 所属页面
    private Long groupId;          // 页面分组ID
    private Long projectId;        // 所属项目ID
    private String componentName;
    private Integer waitTimeout = 5000;
    private Boolean forceAction = false;
    private Integer usageCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UiElement() {}
    public UiElement(String name, String type, String locator, String page) {
        this.name = name; this.elementType = type; this.locatorValue = locator; this.page = page;
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getElementType() { return elementType; } public void setElementType(String t) { this.elementType = t; }
    public String getLocatorStrategy() { return locatorStrategy; } public void setLocatorStrategy(String s) { this.locatorStrategy = s; }
    public String getLocatorValue() { return locatorValue; } public void setLocatorValue(String v) { this.locatorValue = v; }
    public String getPage() { return page; } public void setPage(String p) { this.page = p; }
    public Long getGroupId() { return groupId; } public void setGroupId(Long g) { this.groupId = g; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long p) { this.projectId = p; }
    public String getComponentName() { return componentName; } public void setComponentName(String c) { this.componentName = c; }
    public Integer getWaitTimeout() { return waitTimeout; } public void setWaitTimeout(Integer t) { this.waitTimeout = t; }
    public Boolean getForceAction() { return forceAction; } public void setForceAction(Boolean f) { this.forceAction = f; }
    public Integer getUsageCount() { return usageCount; } public void setUsageCount(Integer c) { this.usageCount = c; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
