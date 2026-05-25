package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.AgentBundlePricing;

import java.util.List;
import java.util.Optional;

public interface AgentBundlePricingRepositoryPort {
    AgentBundlePricing save(AgentBundlePricing pricing);
    Optional<AgentBundlePricing> findByAgentIdAndBundleId(String agentId, String bundleId);
    List<AgentBundlePricing> findByAgentId(String agentId);
    Optional<AgentBundlePricing> findById(String id);
}
