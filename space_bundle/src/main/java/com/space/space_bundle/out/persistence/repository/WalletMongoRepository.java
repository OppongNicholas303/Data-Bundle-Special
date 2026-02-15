package com.space.space_bundle.out.persistence.repository;

import com.space.space_bundle.out.persistence.entity.WalletDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletMongoRepository extends MongoRepository<WalletDocument, String> {
    Optional<WalletDocument> findByUserId(String userId);
}