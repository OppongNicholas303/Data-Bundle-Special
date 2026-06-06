package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "withdrawal_requests")
public class WithdrawalRequest {

    @Id
    private String id;

    @Indexed
    private String agentUserId;     // User.id of the agent

    @Indexed
    private String agentProfileId;  // AgentProfile.id

    private BigDecimal amount;
    private String momoProvider;
    private String momoNumber;
    private String accountName;

    private String status;          // PENDING, APPROVED, REJECTED
    private String reference;       // internal reference e.g. WD_xxxx

    private String adminNote;       // optional note from admin on approve/reject

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
