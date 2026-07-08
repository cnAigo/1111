package org.example.testvue.repository;

import org.example.testvue.entity.UiPageObject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UiPageObjectRepository extends JpaRepository<UiPageObject, Long> {
    List<UiPageObject> findByProjectId(Long projectId);
}
