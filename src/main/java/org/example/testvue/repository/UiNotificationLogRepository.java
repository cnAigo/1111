package org.example.testvue.repository;

import org.example.testvue.entity.UiNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiNotificationLogRepository extends JpaRepository<UiNotificationLog, Long> {
    List<UiNotificationLog> findTop20ByOrderByCreatedAtDesc();
}
