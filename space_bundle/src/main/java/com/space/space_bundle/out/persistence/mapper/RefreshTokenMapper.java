package com.space.space_bundle.out.persistence.mapper;

import com.space.space_bundle.core.entities.RefreshToken;
import com.space.space_bundle.out.persistence.entity.RefreshTokenDocument;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RefreshTokenMapper {

    public RefreshTokenDocument toDocument(RefreshToken refreshToken) {
        if (refreshToken == null) return null;

        return RefreshTokenDocument.builder()
                .id(refreshToken.getId())
                .token(refreshToken.getToken())
                .userId(refreshToken.getUserId())
                .expiryDate(refreshToken.getExpiryDate())
                .createdAt(refreshToken.getCreatedAt())
                .revoked(refreshToken.isRevoked())
                .deviceInfo(refreshToken.getDeviceInfo())
                .ipAddress(refreshToken.getIpAddress())
                .build();
    }

    public RefreshToken toDomain(RefreshTokenDocument document) {
        if (document == null) return null;

        return RefreshToken.builder()
                .id(document.getId())
                .token(document.getToken())
                .userId(document.getUserId())
                .expiryDate(document.getExpiryDate())
                .createdAt(document.getCreatedAt())
                .revoked(document.isRevoked())
                .deviceInfo(document.getDeviceInfo())
                .ipAddress(document.getIpAddress())
                .build();
    }
}

