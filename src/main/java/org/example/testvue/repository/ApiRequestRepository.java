package org.example.testvue.repository;

import org.example.testvue.entity.ApiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long> {
    List<ApiRequest> findByCollectionIdOrderBySortOrder(Long collectionId);
    void deleteByCollectionId(Long collectionId);
    long countByCollectionId(Long collectionId);
}
