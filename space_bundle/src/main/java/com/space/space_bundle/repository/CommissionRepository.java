package com.space.space_bundle.repository;

import com.space.space_bundle.entity.Commission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends MongoRepository<Commission, String> {
    List<Commission> findByAgentId(String agentId);
    List<Commission> findByOrderId(String orderId);
}
