package com.space.space_bundle.out.persistence.repository;


import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.out.persistence.entity.WalletDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpringWalletRepository extends MongoRepository<WalletDocument, String> {
    Optional<WalletDocument> findByUserId(String userId);
}

