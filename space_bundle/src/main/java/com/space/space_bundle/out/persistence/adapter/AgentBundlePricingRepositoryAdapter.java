package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.AgentBundlePricing;
import com.space.space_bundle.core.port.out.AgentBundlePricingRepositoryPort;
import com.space.space_bundle.out.persistence.mapper.AgentBundlePricingMapper;
import com.space.space_bundle.out.persistence.repository.AgentBundlePricingMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentBundlePricingRepositoryAdapter implements AgentBundlePricingRepositoryPort {

    private final AgentBundlePricingMongoRepository mongoRepository;

    @Override
    public AgentBundlePricing save(AgentBundlePricing pricing) {
        return AgentBundlePricingMapper.toDomain(
                mongoRepository.save(AgentBundlePricingMapper.toDocument(pricing)));
    }

    @Override
    public Optional<AgentBundlePricing> findByAgentIdAndBundleId(String agentId, String bundleId) {
        return mongoRepository.findByAgentIdAndBundleId(agentId, bundleId)
                .map(AgentBundlePricingMapper::toDomain);
    }

    @Override
    public List<AgentBundlePricing> findByAgentId(String agentId) {
        return mongoRepository.findByAgentId(agentId).stream()
                .map(AgentBundlePricingMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AgentBundlePricing> findById(String id) {
        return mongoRepository.findById(id).map(AgentBundlePricingMapper::toDomain);
    }
}
