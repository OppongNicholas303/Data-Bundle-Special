package com.space.space_bundle.out.persistence.adapter;


import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.out.persistence.repository.WalletRepositoryPort;
import com.space.space_bundle.out.persistence.entity.WalletDocument;
import com.space.space_bundle.out.persistence.mapper.WalletMapper;
import com.space.space_bundle.out.persistence.repository.SpringWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletAdapter implements WalletRepositoryPort {

    private final SpringWalletRepository repository;

    @Override
    public Optional<Wallet> findByUserId(UUID userId) {
        return repository.findById(userId.toString())
                .map(WalletMapper::toDomain);
    }


    @Override
    public Wallet save(Wallet wallet) {
        WalletDocument saved = repository.save(
                WalletMapper.toDocument(wallet)
        );
        return WalletMapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(WalletMapper::toDomain);
    }

    @Override
    public Optional<Wallet> findById(String walletId) {
        return repository.findById(walletId)
                .map(WalletMapper::toDomain);
    }
}
