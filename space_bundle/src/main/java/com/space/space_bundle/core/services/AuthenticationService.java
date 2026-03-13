package com.space.space_bundle.core.services;

import com.space.space_bundle.core.entities.RefreshToken;
import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.core.port.out.EmailPort;
import com.space.space_bundle.core.port.out.authenticationPort.*;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Core authentication service - contains business logic
 * Framework-independent
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtPort jwtPort;
    private final SecurityAuditPort securityAudit;
    private final WalletService walletService;
    private final EmailPort emailPort;

    /**
     * Authenticate user with username and password
     */
    public AuthenticationResult authenticate(String email, String password, String ipAddress, String deviceInfo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            securityAudit.logFailedLogin(email, ipAddress, "Account locked");
            throw new AuthenticationException("Account is locked due to multiple failed login attempts");
        }

        // Check if account is enabled
        if (!user.isEnabled()) {
            securityAudit.logFailedLogin(email, ipAddress, "Account disabled");
            throw new AuthenticationException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            securityAudit.logFailedLogin(email, ipAddress, "Invalid password");

            if (!user.isAccountNonLocked()) {
                securityAudit.logAccountLocked(email, "Too many failed login attempts");
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

        securityAudit.logSuccessfulLogin(email, ipAddress, deviceInfo);

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

    /**
     * Initiate password reset process
     */
    public void forgotPassword(String email, String frontendUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RegistrationException("User not found with email: " + email));

        // Generate password reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send email with reset link
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken + "&email=" + email;
        String subject = "Reset Your TapData Password";

        // Plain text version for better deliverability
        String textBody = "Hello " + user.getUsername() + ",\n\n" +
                "We received a request to reset your TapData account password. Click the link below to set a new password:\n\n"
                +
                resetLink + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request this, you can safely ignore this email.\n\n" +
                "© 2026 TapData. All rights reserved.";

        // HTML version
        String htmlBody = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }"
                +
                ".container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e1e4e8; border-radius: 12px; background-color: #ffffff; }"
                +
                ".header { text-align: center; padding-bottom: 20px; border-bottom: 1px solid #f1f3f5; }" +
                ".logo { color: #2563EB; font-size: 28px; font-weight: bold; text-decoration: none; }" +
                ".content { padding: 30px 0; }" +
                ".button-container { text-align: center; margin: 30px 0; }" +
                ".button { background-color: #2563EB; color: #ffffff !important; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; display: inline-block; }"
                +
                ".footer { text-align: center; padding-top: 20px; color: #6a737d; font-size: 14px; border-top: 1px solid #f1f3f5; }"
                +
                ".expiry { color: #d73a49; font-size: 13px; margin-top: 10px; }" +
                ".legal { font-size: 11px; color: #999; margin-top: 20px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><div class='logo'>TapData</div></div>" +
                "<div class='content'>" +
                "<h2>Password Reset Request</h2>" +
                "<p>Hello <strong>" + user.getUsername() + "</strong>,</p>" +
                "<p>We received a request to reset your TapData account password. Click the button below to secure your account and set a new password:</p>"
                +
                "<div class='button-container'>" +
                "<a href='" + resetLink + "' class='button'>Reset Password</a>" +
                "</div>" +
                "<p>Or copy and paste this link into your browser:</p>" +
                "<p style='word-break: break-all; color: #0366d6; font-size: 14px;'>" + resetLink + "</p>" +
                "<p class='expiry'>This link will expire in 1 hour for your security.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>If you didn't request this, you can safely ignore this email.</p>" +
                "<p>&copy; 2026 TapData Inc. • Legon, Accra, Ghana</p>" +
                "<div class='legal'>You are receiving this because you requested a password reset for your TapData account.</div>"
                +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        try {
            emailPort.sendMultipartEmail(email, subject, textBody, htmlBody);
        } catch (Exception e) {
            log.error("SMTP Error: Failed to send password reset email to {}: {}", email, e.getMessage(), e);
            throw new RegistrationException("Failed to send password reset email: " + e.getMessage());
        }
    }

    /**
     * Reset user password using reset token
     */
    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RegistrationException("User not found with email: " + email));

        // Validate reset token
        if (!token.equals(user.getPasswordResetToken()) || !user.isPasswordResetTokenValid()) {
            throw new RegistrationException("Invalid or expired password reset token");
        }

        // Validate password strength
        validatePasswordStrength(newPassword);

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordLastChanged(LocalDateTime.now());
        userRepository.save(user);
    }

    // DTOs for service layer
    public record AuthenticationResult(String accessToken, String refreshToken, User user) {
    }

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
