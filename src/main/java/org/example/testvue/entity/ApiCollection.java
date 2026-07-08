package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_collections")
public class ApiCollection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 1000)
    private String description;
    private Long parentId;
    private Long projectId;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ApiCollection() {}
    public ApiCollection(String name, Long projectId) { this.name = name; this.projectId = projectId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long pid) { this.parentId = pid; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long pid) { this.projectId = pid; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer o) { this.sortOrder = o; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
