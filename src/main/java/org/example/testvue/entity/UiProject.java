package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_projects")
public class UiProject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 2000)
    private String description;
    private String baseUrl;
    private Long ownerId;
    private String ownerName;
    private String startDate;
    private String endDate;
    private String status = "active";
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UiProject() {}
    public UiProject(String name, String desc) { this.name = name; this.description = desc; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String u) { this.baseUrl = u; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long id) { this.ownerId = id; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String n) { this.ownerName = n; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String d) { this.startDate = d; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String d) { this.endDate = d; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
