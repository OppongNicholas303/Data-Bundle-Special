package com.space.space_bundle.repository;

import com.space.space_bundle.entity.AgentCheckerPricing;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentCheckerPricingRepository extends MongoRepository<AgentCheckerPricing, String> {
    Optional<AgentCheckerPricing> findByAgentIdAndServiceName(String agentId, String serviceName);
    List<AgentCheckerPricing> findByAgentId(String agentId);
    List<AgentCheckerPricing> findByServiceName(String serviceName);
}
