package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.AgentProfileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentProfileMongoRepository extends MongoRepository<AgentProfileDocument, String> {
    Optional<AgentProfileDocument> findByUserId(String userId);
    Optional<AgentProfileDocument> findByReferralCode(String referralCode);
    boolean existsByUserId(String userId);
    boolean existsByReferralCode(String referralCode);
}
