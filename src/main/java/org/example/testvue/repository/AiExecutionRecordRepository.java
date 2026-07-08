package org.example.testvue.repository;

import org.example.testvue.entity.AiExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiExecutionRecordRepository extends JpaRepository<AiExecutionRecord, Long> {
    List<AiExecutionRecord> findAllByOrderByCreatedAtDesc();
}
