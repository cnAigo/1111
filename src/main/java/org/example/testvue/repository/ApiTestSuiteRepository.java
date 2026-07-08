package org.example.testvue.repository;

import org.example.testvue.entity.ApiTestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiTestSuiteRepository extends JpaRepository<ApiTestSuite, Long> {
    List<ApiTestSuite> findByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
