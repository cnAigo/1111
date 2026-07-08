package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ui_notification_logs")
public class UiNotificationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private String status;
    @Column(length = 2000)
    private String message;
    private String recipient;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getType() { return type; } public void setType(String t) { this.type = t; }
    public String getStatus() { return status; } public void setStatus(String s) { this.status = s; }
    public String getMessage() { return message; } public void setMessage(String m) { this.message = m; }
    public String getRecipient() { return recipient; } public void setRecipient(String r) { this.recipient = r; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
}
