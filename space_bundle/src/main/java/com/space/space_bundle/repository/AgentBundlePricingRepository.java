package com.space.space_bundle.repository;

import com.space.space_bundle.entity.AgentBundlePricing;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentBundlePricingRepository extends MongoRepository<AgentBundlePricing, String> {
    Optional<AgentBundlePricing> findByAgentIdAndBundleId(String agentId, String bundleId);
    List<AgentBundlePricing> findByAgentId(String agentId);
}
