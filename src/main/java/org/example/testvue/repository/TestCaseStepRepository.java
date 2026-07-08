package org.example.testvue.repository;

import org.example.testvue.entity.TestCaseStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseStepRepository extends JpaRepository<TestCaseStep, Long> {
    List<TestCaseStep> findByTestCaseIdOrderByStepOrderAsc(Long testCaseId);
    void deleteByTestCaseId(Long testCaseId);
}
