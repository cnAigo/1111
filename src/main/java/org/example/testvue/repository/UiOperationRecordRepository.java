package org.example.testvue.repository;

import org.example.testvue.entity.UiOperationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiOperationRecordRepository extends JpaRepository<UiOperationRecord, Long> {
    List<UiOperationRecord> findTop20ByOrderByCreatedAtDesc();
}
