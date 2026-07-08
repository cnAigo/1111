package org.example.testvue.repository;

import org.example.testvue.entity.UiTestScript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiTestScriptRepository extends JpaRepository<UiTestScript, Long> {
    List<UiTestScript> findByProjectId(Long projectId);
}
