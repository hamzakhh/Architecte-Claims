package com.example.backend.repository;

import com.example.backend.model.ClaimHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimHistoryRepository extends MongoRepository<ClaimHistory, String> {
    List<ClaimHistory> findByClaimIdOrderByCreatedAtAsc(String claimId);
    List<ClaimHistory> findByClaimId(String claimId);
}
