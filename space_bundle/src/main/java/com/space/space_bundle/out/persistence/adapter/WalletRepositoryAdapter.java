package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.Wallet;
import com.space.space_bundle.core.port.out.WalletRepositoryPort;
import com.space.space_bundle.out.persistence.entity.WalletDocument;
import com.space.space_bundle.out.persistence.mapper.WalletMapper;
import com.space.space_bundle.out.persistence.repository.WalletMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepositoryPort {

    private final WalletMongoRepository mongoRepository;

    @Override
    public Wallet save(Wallet wallet) {
        WalletDocument document = WalletMapper.toDocument(wallet);
        WalletDocument saved = mongoRepository.save(document);
        return WalletMapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findByUserId(String userId) {
        return mongoRepository.findByUserId(userId)
                .map(WalletMapper::toDomain);
    }

}