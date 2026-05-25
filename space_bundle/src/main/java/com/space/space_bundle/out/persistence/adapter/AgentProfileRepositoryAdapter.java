package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.AgentProfile;
import com.space.space_bundle.core.port.out.AgentProfileRepositoryPort;
import com.space.space_bundle.out.persistence.mapper.AgentProfileMapper;
import com.space.space_bundle.out.persistence.repository.AgentProfileMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgentProfileRepositoryAdapter implements AgentProfileRepositoryPort {

    private final AgentProfileMongoRepository mongoRepository;

    @Override
    public AgentProfile save(AgentProfile agentProfile) {
        return AgentProfileMapper.toDomain(
                mongoRepository.save(AgentProfileMapper.toDocument(agentProfile)));
    }

    @Override
    public Optional<AgentProfile> findByUserId(String userId) {
        return mongoRepository.findByUserId(userId).map(AgentProfileMapper::toDomain);
    }

    @Override
    public Optional<AgentProfile> findByReferralCode(String referralCode) {
        return mongoRepository.findByReferralCode(referralCode).map(AgentProfileMapper::toDomain);
    }

    @Override
    public Optional<AgentProfile> findById(String id) {
        return mongoRepository.findById(id).map(AgentProfileMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return mongoRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsByReferralCode(String referralCode) {
        return mongoRepository.existsByReferralCode(referralCode);
    }
}
