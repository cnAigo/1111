package org.example.testvue.repository;

import org.example.testvue.entity.UiTestEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiTestEnvironmentRepository extends JpaRepository<UiTestEnvironment, Long> {
    List<UiTestEnvironment> findByProjectId(Long projectId);
    List<UiTestEnvironment> findByIsActiveTrue();
}
