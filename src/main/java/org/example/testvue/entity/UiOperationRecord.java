package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_operation_records")
public class UiOperationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String operationType;
    private String description;
    private String userName = "admin";
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getOperationType() { return operationType; } public void setOperationType(String t) { this.operationType = t; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getUserName() { return userName; } public void setUserName(String n) { this.userName = n; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
