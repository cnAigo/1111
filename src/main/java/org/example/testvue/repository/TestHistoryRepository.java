package org.example.testvue.repository;

import org.example.testvue.entity.TestHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestHistoryRepository extends JpaRepository<TestHistory, Long> {
    List<TestHistory> findAllByOrderByCreateTimeDesc(Pageable pageable);
    TestHistory findTopByOrderByCreateTimeDesc();
    TestHistory findByTaskId(String taskId);
    void deleteByTaskId(String taskId);
}
