package org.example.testvue.repository;

import org.example.testvue.entity.ApiProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiProjectRepository extends JpaRepository<ApiProject, Long> {
}
