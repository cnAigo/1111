package org.example.testvue.repository;

import org.example.testvue.entity.UiTestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiTestExecutionRepository extends JpaRepository<UiTestExecution, Long> {
    List<UiTestExecution> findByProjectIdOrderByExecutedAtDesc(Long projectId);
    List<UiTestExecution> findAllByOrderByExecutedAtDesc();
}
