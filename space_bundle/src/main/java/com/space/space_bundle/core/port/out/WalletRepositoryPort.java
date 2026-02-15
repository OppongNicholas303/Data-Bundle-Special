package com.space.space_bundle.core.port.out;

import com.space.space_bundle.core.entities.Wallet;
import java.util.Optional;

public interface WalletRepositoryPort {
    Wallet save(Wallet wallet);
    Optional<Wallet> findByUserId(String userId);
}