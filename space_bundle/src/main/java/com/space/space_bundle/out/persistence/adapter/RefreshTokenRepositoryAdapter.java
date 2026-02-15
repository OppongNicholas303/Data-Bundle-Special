package com.space.space_bundle.out.persistence.adapter;

import com.space.space_bundle.core.entities.RefreshToken;
import com.space.space_bundle.core.port.out.authenticationPort.RefreshTokenRepositoryPort;
import com.space.space_bundle.out.persistence.entity.RefreshTokenDocument;
import com.space.space_bundle.out.persistence.mapper.RefreshTokenMapper;
import com.space.space_bundle.out.persistence.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB adapter for RefreshToken repository operations
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenDocument document = RefreshTokenMapper.toDocument(token);
        RefreshTokenDocument saved = refreshTokenRepository.save(document);
        return RefreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public void deleteByUserId(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.findAll().stream()
                .filter(doc -> userId.equals(doc.getUserId()))
                .forEach(doc -> {
                    doc.setRevoked(true);
                    refreshTokenRepository.save(doc);
                });
    }
}