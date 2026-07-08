package org.example.testvue.repository;

import org.example.testvue.entity.AiPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiPromptTemplateRepository extends JpaRepository<AiPromptTemplate, Long> {
    List<AiPromptTemplate> findByIsActiveTrueOrderByCreatedAtDesc();
    List<AiPromptTemplate> findByTypeOrderByCreatedAtDesc(String type);
}
