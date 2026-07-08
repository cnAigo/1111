package org.example.testvue.repository;

import org.example.testvue.entity.TestClassStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TestClassStatsRepository extends JpaRepository<TestClassStats, Long> {
    Optional<TestClassStats> findByClassName(String className);
}
