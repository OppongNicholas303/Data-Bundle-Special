package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sms_sender_id_requests")
@CompoundIndex(name = "sms_sender_id_user_name", def = "{'userId': 1, 'senderId': 1}", unique = true)
public class SmsSenderIdRequest {
    @Id private String id;
    private String userId;
    private String senderId;
    private String status; // PENDING, APPROVED, REJECTED
    private String providerStatus; // PENDING, APPROVED, REJECTED, UNKNOWN
    private String reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
