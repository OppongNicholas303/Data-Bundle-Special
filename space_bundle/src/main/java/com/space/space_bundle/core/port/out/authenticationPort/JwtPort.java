package com.space.space_bundle.core.port.out.authenticationPort;

import com.space.space_bundle.core.entities.User;

/**
 * Output port for JWT operations
 */
public interface JwtPort {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    boolean validateToken(String token);
    String extractUsername(String token);
    String extractUserId(String token);
}
