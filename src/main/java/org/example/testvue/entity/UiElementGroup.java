package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_element_groups")
public class UiElementGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long parentId;
    private Long projectId;
    private LocalDateTime createdAt = LocalDateTime.now();

    public UiElementGroup() {}
    public UiElementGroup(String name) { this.name = name; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long pid) { this.parentId = pid; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long pid) { this.projectId = pid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
