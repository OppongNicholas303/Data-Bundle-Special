package com.space.space_bundle.core.services;


import com.space.space_bundle.core.entities.RefreshToken;
import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.core.port.out.authenticationPort.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Core authentication service - contains business logic
 * Framework-independent
 */
@RequiredArgsConstructor
public class AuthenticationService {


    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtPort jwtPort;
    private final SecurityAuditPort securityAudit;
    private final  WalletService walletService;

    /**
     * Authenticate user with username and password
     */
    public AuthenticationResult authenticate(String username, String password, String ipAddress, String deviceInfo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            securityAudit.logFailedLogin(username, ipAddress, "Account locked");
            throw new AuthenticationException("Account is locked due to multiple failed login attempts");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            securityAudit.logFailedLogin(username, ipAddress, "Account disabled");
            throw new AuthenticationException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            securityAudit.logFailedLogin(username, ipAddress, "Invalid password");

            if (!user.isAccountNonLocked()) {
                securityAudit.logAccountLocked(username, "Too many failed login attempts");
            }

            throw new AuthenticationException("Invalid credentials");
        }

        // Check password expiration
        if (user.isPasswordExpired()) {
            throw new AuthenticationException("Password has expired. Please reset your password.");
        }

        // Reset failed attempts on successful login
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtPort.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, ipAddress, deviceInfo);

        securityAudit.logSuccessfulLogin(username, ipAddress, deviceInfo);

        return new AuthenticationResult(accessToken, refreshToken, user);
    }

    /**
     * Register new user
     */
    public User register(String username, String email, String password, String phoneNumber) {
        // Validate username uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new RegistrationException("Username already exists");
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("Email already exists");
        }

        // Validate password strength
        validatePasswordStrength(password);

        // Create user
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber(phoneNumber)
                .roles(Set.of(User.Role.ROLE_USER))
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .passwordLastChanged(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        walletService.createWallet(user.getId());

        return userRepository.save(user);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthenticationResult refreshToken(String refreshTokenStr, String ipAddress) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new TokenException("Refresh token is expired or revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new TokenException("User not found"));

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new TokenException("User account is not active");
        }

        // Generate new access token
        String newAccessToken = jwtPort.generateAccessToken(user);

        securityAudit.logTokenRefresh(user.getUsername(), ipAddress);

        return new AuthenticationResult(newAccessToken, refreshTokenStr, user);
    }

    /**
     * Logout user and revoke refresh token
     */
    public void logout(String refreshToken, String username, String ipAddress) {
        refreshTokenRepository.deleteByToken(refreshToken);
        securityAudit.logLogout(username, ipAddress);
    }

    /**
     * Change user password
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        validatePasswordStrength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLastChanged(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllUserTokens(userId);

        securityAudit.logPasswordChanged(user.getUsername());
    }

    private String createRefreshToken(User user, String ipAddress, String deviceInfo) {
        // Revoke existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RegistrationException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RegistrationException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RegistrationException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new RegistrationException("Password must contain at least one digit");
        }

        if (!password.matches(".*[@#$%^&+=!].*")) {
            throw new RegistrationException("Password must contain at least one special character (@#$%^&+=!)");
        }
    }

    // DTOs for service layer
    public record AuthenticationResult(String accessToken, String refreshToken, User user) {}

    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {
            super(message);
        }
    }

    public static class TokenException extends RuntimeException {
        public TokenException(String message) {
            super(message);
        }
    }
}
