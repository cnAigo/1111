package org.example.testvue.repository;

import org.example.testvue.entity.UiScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiScheduledTaskRepository extends JpaRepository<UiScheduledTask, Long> {
    List<UiScheduledTask> findByStatus(String status);
}
