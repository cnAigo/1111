package org.example.testvue.repository;

import org.example.testvue.entity.ApiScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiScheduledTaskRepository extends JpaRepository<ApiScheduledTask, Long> {
}
