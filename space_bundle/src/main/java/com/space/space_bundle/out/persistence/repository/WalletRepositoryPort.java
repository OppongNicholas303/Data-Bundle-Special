package com.space.space_bundle.out.persistence.repository;



import com.space.space_bundle.core.entities.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepositoryPort {

    Optional<Wallet> findByUserId(UUID userId);

    Wallet save(Wallet wallet);

    Optional<Wallet> findByUserId(String userId);

    Optional<Wallet> findById(String walletId);
}
