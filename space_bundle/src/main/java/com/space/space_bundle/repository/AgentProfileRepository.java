package com.space.space_bundle.repository;

import com.space.space_bundle.entity.AgentProfile;
import com.space.space_bundle.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentProfileRepository extends MongoRepository<AgentProfile, String> {
    Optional<AgentProfile> findByUserId(String userId);
    Optional<AgentProfile> findByReferralCode(String referralCode);
    boolean existsByUserId(String userId);
    boolean existsByReferralCode(String referralCode);
}
