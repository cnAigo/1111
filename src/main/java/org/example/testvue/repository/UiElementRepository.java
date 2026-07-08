package org.example.testvue.repository;

import org.example.testvue.entity.UiElement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiElementRepository extends JpaRepository<UiElement, Long> {
    List<UiElement> findByPage(String page);
    List<UiElement> findByGroupId(Long groupId);
    List<UiElement> findByProjectId(Long projectId);
}
