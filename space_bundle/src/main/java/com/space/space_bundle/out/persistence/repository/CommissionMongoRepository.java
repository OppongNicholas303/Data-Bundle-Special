package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.CommissionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionMongoRepository extends MongoRepository<CommissionDocument, String> {
    Optional<CommissionDocument> findByOrderId(String orderId);
    List<CommissionDocument> findByAgentId(String agentId);
}
