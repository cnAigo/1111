package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_configs")
public class NotificationConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String type;
    @Column(columnDefinition = "TEXT")
    private String config;
    private Boolean enabled = true;
    private Long taskId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NotificationConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public String getConfig() { return config; }
    public void setConfig(String c) { this.config = c; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean e) { this.enabled = e; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long tid) { this.taskId = tid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
