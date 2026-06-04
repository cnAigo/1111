package org.example.testvue.repository;

import org.example.testvue.entity.TestCaseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseDetailRepository extends JpaRepository<TestCaseDetail, Long> {
    List<TestCaseDetail> findByClassName(String className);
    List<TestCaseDetail> findByCaseId(String caseId);
    List<TestCaseDetail> findByModule(String module);
    List<TestCaseDetail> findByCaseIdIn(List<String> caseIds);
}
