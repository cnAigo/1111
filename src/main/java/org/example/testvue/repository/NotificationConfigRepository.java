package org.example.testvue.repository;

import org.example.testvue.entity.NotificationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, Long> {
    List<NotificationConfig> findByTaskId(Long taskId);
}
