package com.space.space_bundle.in.web.controller;

import com.space.space_bundle.core.entities.User;
import com.space.space_bundle.core.services.AuthenticationService;
import com.space.space_bundle.in.web.dto.*;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import com.space.space_bundle.out.security.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Authentication REST Controller
 * Handles user authentication, registration, and token management
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${app.frontend-url:http://localhost:8081}")
    private String frontendUrl;

    /**
     * POST /api/auth/login - Authenticate user and return JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login attempt for username: {}", request.getUsername());

        String ipAddress = getClientIpAddress(httpRequest);
        String deviceInfo = getUserAgent(httpRequest);

        AuthenticationService.AuthenticationResult result = authenticationService.authenticate(
                request.getUsername(),
                request.getPassword(),
                ipAddress,
                deviceInfo);

        AuthenticationResponse response = buildAuthenticationResponse(result);

        log.info("Login successful for username: {}", request.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * POST /api/auth/register - Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse.UserInfo>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration attempt for username: {}", request.getUsername());

        User user = authenticationService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getPhoneNumber());

        AuthenticationResponse.UserInfo userInfo = mapToUserInfo(user);

        log.info("Registration successful for username: {}", request.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userInfo));
    }

    /**
     * POST /api/auth/refresh - Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.info("Token refresh attempt");

        String ipAddress = getClientIpAddress(httpRequest);

        AuthenticationService.AuthenticationResult result = authenticationService.refreshToken(
                request.getRefreshToken(),
                ipAddress);

        AuthenticationResponse response = buildAuthenticationResponse(result);

        log.info("Token refresh successful");

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * POST /api/auth/logout - Logout user and revoke refresh token
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        String username = userDetails.getUsername();
        String ipAddress = getClientIpAddress(httpRequest);

        log.info("Logout attempt for username: {}", username);

        authenticationService.logout(request.getRefreshToken(), username, ipAddress);

        log.info("Logout successful for username: {}", username);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * POST /api/auth/change-password - Change user password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info("Password change attempt for user ID: {}", userId);

        authenticationService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword());

        log.info("Password changed successfully for user ID: {}", userId);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * POST /api/auth/forgot-password - Initiate password reset process
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password request for email: {}", request.getEmail());

        authenticationService.forgotPassword(request.getEmail(), frontendUrl);

        log.info("Password reset email sent for email: {}", request.getEmail());

        return ResponseEntity.ok(ApiResponse.success("Password reset link has been sent to your email", null));
    }

    /**
     * POST /api/auth/reset-password - Reset user password using reset token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset attempt for email: {}", request.getEmail());

        authenticationService.resetPassword(
                request.getEmail(),
                request.getToken(),
                request.getNewPassword());

        log.info("Password reset successful for email: {}", request.getEmail());

        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    /**
     * GET /api/auth/me - Get current user information
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthenticationResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        User user = userDetails.getUser();
        AuthenticationResponse.UserInfo userInfo = mapToUserInfo(user);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    // Helper methods

    private AuthenticationResponse buildAuthenticationResponse(AuthenticationService.AuthenticationResult result) {
        return AuthenticationResponse.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                .user(mapToUserInfo(result.user()))
                .build();
    }

    private AuthenticationResponse.UserInfo mapToUserInfo(User user) {
        return AuthenticationResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}