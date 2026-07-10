package com.space.space_bundle.repository;

import com.space.space_bundle.entity.Commission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface CommissionRepository extends MongoRepository<Commission, String> {
    List<Commission> findByAgentId(String agentId);
    List<Commission> findByOrderId(String orderId);
    List<Commission> findByAgentIdAndCreatedAtBetween(String agentId, LocalDateTime from, LocalDateTime to);
    List<Commission> findByAgentIdAndStatus(String agentId, String status);
    List<Commission> findByAgentIdAndStatusAndCreatedAtBetween(String agentId, String status, LocalDateTime from, LocalDateTime to);
    List<Commission> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
