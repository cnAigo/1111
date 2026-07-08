package org.example.testvue.repository;

import org.example.testvue.entity.ApiRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiRequestHistoryRepository extends JpaRepository<ApiRequestHistory, Long> {
    List<ApiRequestHistory> findTop20ByOrderByCreatedAtDesc();
    List<ApiRequestHistory> findAllByOrderByCreatedAtDesc();
    void deleteByIdIn(List<Long> ids);
    void deleteByRequestId(Long requestId);
    long countByRequestId(Long requestId);
}
