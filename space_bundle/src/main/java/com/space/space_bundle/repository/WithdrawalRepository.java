package com.space.space_bundle.repository;

import com.space.space_bundle.entity.WithdrawalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRepository extends MongoRepository<WithdrawalRequest, String> {
    List<WithdrawalRequest> findByAgentUserIdOrderByCreatedAtDesc(String agentUserId);
    List<WithdrawalRequest> findByAgentProfileIdOrderByCreatedAtDesc(String agentProfileId);
    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();
}
