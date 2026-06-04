package org.example.testvue.repository;

import org.example.testvue.entity.TestConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestConfigRepository extends JpaRepository<TestConfigEntity, Long> {
}
