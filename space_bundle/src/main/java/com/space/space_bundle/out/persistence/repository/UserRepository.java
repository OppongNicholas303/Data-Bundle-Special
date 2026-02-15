package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for User
 */
@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByUsername(String username);
    Optional<UserDocument> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}