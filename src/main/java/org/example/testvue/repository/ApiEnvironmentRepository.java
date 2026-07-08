package org.example.testvue.repository;

import org.example.testvue.entity.ApiEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiEnvironmentRepository extends JpaRepository<ApiEnvironment, Long> {
    List<ApiEnvironment> findByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
