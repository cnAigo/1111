package org.example.testvue.repository;

import org.example.testvue.entity.UiTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiTestCaseRepository extends JpaRepository<UiTestCase, Long> {
    List<UiTestCase> findByProjectId(Long projectId);
}
