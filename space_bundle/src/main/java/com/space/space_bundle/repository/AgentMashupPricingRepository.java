package com.space.space_bundle.repository;

import com.space.space_bundle.entity.AgentMashupPricing;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AgentMashupPricingRepository extends MongoRepository<AgentMashupPricing, String> {

    Optional<AgentMashupPricing> findByAgentIdAndMashupBundleId(String agentId, String mashupBundleId);

    List<AgentMashupPricing> findByAgentId(String agentId);

    List<AgentMashupPricing> findByMashupBundleId(String mashupBundleId);
}
