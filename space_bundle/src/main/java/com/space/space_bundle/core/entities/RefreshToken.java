package com.space.space_bundle.core.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Core RefreshToken entity for secure token refresh mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private String id;
    private String token;
    private String userId;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private boolean revoked;
    private String deviceInfo;
    private String ipAddress;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
