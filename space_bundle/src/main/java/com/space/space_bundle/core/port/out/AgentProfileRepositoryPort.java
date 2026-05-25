package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.AgentProfile;

import java.util.Optional;

public interface AgentProfileRepositoryPort {
    AgentProfile save(AgentProfile agentProfile);
    Optional<AgentProfile> findByUserId(String userId);
    Optional<AgentProfile> findById(String id);
    Optional<AgentProfile> findByReferralCode(String referralCode);
    boolean existsByUserId(String userId);
    boolean existsByReferralCode(String referralCode);
}
