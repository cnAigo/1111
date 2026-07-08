package org.example.testvue.repository;

import org.example.testvue.entity.ApiTestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiTestExecutionRepository extends JpaRepository<ApiTestExecution, Long> {
    List<ApiTestExecution> findBySuiteIdOrderByExecutedAtDesc(Long suiteId);
    List<ApiTestExecution> findAllByOrderByExecutedAtDesc();
    void deleteBySuiteId(Long suiteId);
}
