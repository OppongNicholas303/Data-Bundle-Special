package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = false)
    private String username;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;
    private String phoneNumber;
    private Set<String> roles;
    private boolean enabled;
    private boolean accountNonLocked;

    @JsonIgnore
    private int failedLoginAttempts;
    @JsonIgnore
    private LocalDateTime lastFailedLogin;
    private LocalDateTime lastSuccessfulLogin;
    @JsonIgnore
    private LocalDateTime passwordLastChanged;
    @JsonIgnore
    private String passwordResetToken;
    @JsonIgnore
    private LocalDateTime passwordResetTokenExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        ROLE_USER, ROLE_ADMIN, ROLE_SUPPORT, ROLE_AGENT
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = LocalDateTime.now();
        if (this.failedLoginAttempts >= 5) this.accountNonLocked = false;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastSuccessfulLogin = LocalDateTime.now();
    }

    public boolean isPasswordExpired() {
        // Password expiry policy disabled
        return false;
    }

    public boolean isPasswordResetTokenValid() {
        if (passwordResetToken == null || passwordResetTokenExpiry == null) return false;
        return passwordResetTokenExpiry.isAfter(LocalDateTime.now());
    }
}
