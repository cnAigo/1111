package org.example.testvue.repository;

import org.example.testvue.entity.TestHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestHistoryRepository extends JpaRepository<TestHistory, Long> {
    Page<TestHistory> findAllByOrderByCreateTimeDesc(Pageable pageable);
    Page<TestHistory> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);
    TestHistory findTopByOrderByCreateTimeDesc();
    TestHistory findByTaskId(String taskId);
    void deleteByTaskId(String taskId);
}
