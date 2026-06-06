package com.space.space_bundle.service;

import com.space.space_bundle.entity.RefreshToken;
import com.space.space_bundle.entity.User;
import com.space.space_bundle.repository.RefreshTokenRepository;
import com.space.space_bundle.repository.UserRepository;
import com.space.space_bundle.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WalletService walletService;
    private final EmailService emailService;

    public record AuthResult(String accessToken, String refreshToken, User user) {}

    public AuthResult login(String usernameOrEmail, String password, String ip, String device) {
        // Support login with either username or email
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!user.isAccountNonLocked()) throw new AuthException("Account is locked");
        if (!user.isEnabled()) throw new AuthException("Account is disabled");

        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new AuthException("Invalid credentials");
        }

        if (user.isPasswordExpired()) throw new AuthException("Password expired. Please reset.");

        user.resetFailedLoginAttempts();
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = createRefreshToken(user, ip, device);
        log.info("SECURITY: Login success - {}, IP: {}", usernameOrEmail, ip);
        return new AuthResult(accessToken, refreshToken, user);
    }

    public User register(String username, String email, String password, String phoneNumber) {
        if (userRepository.existsByUsername(username)) throw new RegistrationException("Username already exists");
        if (userRepository.existsByEmail(email)) throw new RegistrationException("Email already exists");
        validatePassword(password);

        User user = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .username(username).email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber(phoneNumber)
                .roles(Set.of(User.Role.ROLE_USER.name()))
                .enabled(true).accountNonLocked(true)
                .failedLoginAttempts(0)
                .passwordLastChanged(LocalDateTime.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        walletService.createWallet(user.getId());
        return user;
    }

    public AuthResult refreshToken(String token, String ip) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Invalid refresh token"));
        if (!rt.isValid()) throw new TokenException("Refresh token expired or revoked");

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new TokenException("User not found"));
        if (!user.isEnabled() || !user.isAccountNonLocked())
            throw new TokenException("Account not active");

        return new AuthResult(jwtProvider.generateAccessToken(user), token, user);
    }

    public void logout(String token, String username, String ip) {
        refreshTokenRepository.deleteByToken(token);
        log.info("SECURITY: Logout - {}, IP: {}", username, ip);
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new AuthException("Current password is incorrect");
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLastChanged(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(userId);
    }

    public void forgotPassword(String email, String frontendUrl) {
        // Always return success to prevent user enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendPasswordReset(email, user.getUsername(), frontendUrl, token);
        });
    }

    public void resetPassword(String email, String token, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RegistrationException("Invalid or expired reset token"));
        // Timing-safe comparison to prevent timing attacks
        if (!timingSafeEquals(token, user.getPasswordResetToken()) || !user.isPasswordResetTokenValid())
            throw new RegistrationException("Invalid or expired reset token");
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordLastChanged(LocalDateTime.now());
        userRepository.save(user);
    }

    /** Constant-time string comparison to prevent timing attacks on token comparison */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    private String createRefreshToken(User user, String ip, String device) {
        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken rt = refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false).ipAddress(ip).deviceInfo(device)
                .build());
        return rt.getToken();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8)
            throw new RegistrationException("Password must be at least 8 characters");
        if (!password.matches(".*[A-Z].*")) throw new RegistrationException("Password needs an uppercase letter");
        if (!password.matches(".*[a-z].*")) throw new RegistrationException("Password needs a lowercase letter");
        if (!password.matches(".*\\d.*")) throw new RegistrationException("Password needs a digit");
        if (!password.matches(".*[@#$%^&+=!].*")) throw new RegistrationException("Password needs a special character");
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String msg) { super(msg); }
    }
    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String msg) { super(msg); }
    }
    public static class TokenException extends RuntimeException {
        public TokenException(String msg) { super(msg); }
    }
}
