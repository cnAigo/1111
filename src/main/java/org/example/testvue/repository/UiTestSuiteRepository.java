package org.example.testvue.repository;

import org.example.testvue.entity.UiTestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiTestSuiteRepository extends JpaRepository<UiTestSuite, Long> {
    List<UiTestSuite> findByProjectId(Long projectId);
}
