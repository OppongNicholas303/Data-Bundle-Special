package com.space.space_bundle.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @JsonIgnore
    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    @JsonIgnore
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    @JsonIgnore
    private boolean revoked;
    @JsonIgnore
    private String deviceInfo;
    @JsonIgnore
    private String ipAddress;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
