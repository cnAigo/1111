package org.example.testvue.repository;

import org.example.testvue.entity.UiElementGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiElementGroupRepository extends JpaRepository<UiElementGroup, Long> {
    List<UiElementGroup> findByProjectId(Long projectId);
    List<UiElementGroup> findByParentId(Long parentId);
}
