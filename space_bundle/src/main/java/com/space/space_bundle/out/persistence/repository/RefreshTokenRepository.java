package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.RefreshTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for RefreshToken
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshTokenDocument, String> {
    Optional<RefreshTokenDocument> findByToken(String token);
    void deleteByUserId(String userId);
    void deleteByToken(String token);
    boolean existsByToken(String token);
}