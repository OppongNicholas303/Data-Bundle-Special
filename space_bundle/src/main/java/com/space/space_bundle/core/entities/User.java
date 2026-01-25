package com.space.space_bundle.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Core User entity - framework-free domain model
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String username;
    private String email;
    private String password; // Hashed
    private String phoneNumber;
    private Set<Role> roles;
    private boolean enabled;
    private boolean accountNonLocked;
    private int failedLoginAttempts;
    private LocalDateTime lastFailedLogin;
    private LocalDateTime lastSuccessfulLogin;
    private LocalDateTime passwordLastChanged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        ROLE_USER,
        ROLE_ADMIN,
        ROLE_SUPPORT
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = LocalDateTime.now();
        if (this.failedLoginAttempts >= 5) {
            this.accountNonLocked = false;
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastSuccessfulLogin = LocalDateTime.now();
    }

    public boolean isPasswordExpired() {
        if (passwordLastChanged == null) {
            return false;
        }
        // Password expires after 90 days
        return passwordLastChanged.plusDays(90).isBefore(LocalDateTime.now());
    }
}