package com.space.space_bundle.core.port.out.authenticationPort;

import com.space.space_bundle.core.entities.RefreshToken;

import java.util.Optional;

/**
 * Output port for refresh token operations
 */
public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(String userId);
    void deleteByToken(String token);
    void revokeAllUserTokens(String userId);
}