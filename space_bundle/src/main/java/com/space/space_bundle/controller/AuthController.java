package com.space.space_bundle.controller;

import com.space.space_bundle.entity.User;
import com.space.space_bundle.dto.*;
import com.space.space_bundle.security.CustomUserDetailsService;
import com.space.space_bundle.service.AuthService;
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

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${app.frontend-url:http://localhost:8081}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        var result = authService.login(request.getUsername(), request.getPassword(),
                getIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("Login successful", buildResponse(result)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse.UserInfo>> register(
            @Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.getUsername(), request.getEmail(),
                request.getPassword(), request.getPhoneNumber());

        System.out.println("register");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", toUserInfo(user)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        var result = authService.refreshToken(request.getRefreshToken(), getIp(http));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", buildResponse(result)));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            HttpServletRequest http) {
        authService.logout(request.getRefreshToken(), userDetails.getUsername(), getIp(http));
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        authService.changePassword(userDetails.getUserId(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail(), frontendUrl);
        return ResponseEntity.ok(ApiResponse.success("Reset link sent", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthenticationResponse.UserInfo>> me(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(toUserInfo(userDetails.getUser())));
    }

    private AuthenticationResponse buildResponse(AuthService.AuthResult result) {
        return AuthenticationResponse.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .user(toUserInfo(result.user()))
                .build();
    }

    private AuthenticationResponse.UserInfo toUserInfo(User user) {
        return AuthenticationResponse.UserInfo.builder()
                .id(user.getId()).username(user.getUsername())
                .email(user.getEmail()).phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles())
                .build();
    }

    private String getIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
