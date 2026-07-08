package org.example.testvue.repository;

import org.example.testvue.entity.AiCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiCaseRepository extends JpaRepository<AiCase, Long> {
    List<AiCase> findAllByOrderByCreatedAtDesc();
}
