package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.AgentBundlePricingDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentBundlePricingMongoRepository extends MongoRepository<AgentBundlePricingDocument, String> {
    Optional<AgentBundlePricingDocument> findByAgentIdAndBundleId(String agentId, String bundleId);
    List<AgentBundlePricingDocument> findByAgentId(String agentId);
}
