package org.example.testvue.repository;

import org.example.testvue.entity.ApiCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiCollectionRepository extends JpaRepository<ApiCollection, Long> {
    List<ApiCollection> findByProjectIdOrderBySortOrder(Long projectId);
    List<ApiCollection> findByParentIdOrderBySortOrder(Long parentId);
    void deleteByProjectId(Long projectId);
    List<ApiCollection> findByNameContainingIgnoreCase(String name);
}
