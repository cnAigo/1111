package org.example.testvue.repository;

import org.example.testvue.entity.AiModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiModelConfigRepository extends JpaRepository<AiModelConfig, Long> {
    Optional<AiModelConfig> findByIsActiveTrue();
}
