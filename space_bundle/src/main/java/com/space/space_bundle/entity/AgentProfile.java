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
@Document(collection = "agent_profiles")
public class AgentProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String businessName;

    @Indexed(unique = true)
    private String referralCode;

    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
