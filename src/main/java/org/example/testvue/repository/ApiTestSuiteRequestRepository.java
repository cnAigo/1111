package org.example.testvue.repository;

import org.example.testvue.entity.ApiTestSuiteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiTestSuiteRequestRepository extends JpaRepository<ApiTestSuiteRequest, Long> {
    List<ApiTestSuiteRequest> findByTestSuiteIdOrderByOrderNo(Long testSuiteId);
    void deleteByTestSuiteIdAndRequestId(Long testSuiteId, Long requestId);
    void deleteByTestSuiteId(Long testSuiteId);
    boolean existsByTestSuiteIdAndRequestId(Long testSuiteId, Long requestId);
    long countByTestSuiteId(Long testSuiteId);
}
