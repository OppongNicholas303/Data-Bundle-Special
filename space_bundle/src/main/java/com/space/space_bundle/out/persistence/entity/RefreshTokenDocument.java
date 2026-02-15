package com.space.space_bundle.out.persistence.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for RefreshToken
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshTokenDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    @Indexed(expireAfter = "0s")   // or "PT0S" (ISO-8601), but "0s" is clearest
    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;
    private boolean revoked;
    private String deviceInfo;
    private String ipAddress;

}