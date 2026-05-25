package com.space.space_bundle.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "commissions")
public class CommissionDocument {
    @Id
    private String id;
    @Indexed
    private String agentId;
    @Indexed(unique = true)
    private String orderId;
    private BigDecimal baseAmount;
    private BigDecimal sellingAmount;
    private BigDecimal profit;
    private String status;
    private LocalDateTime createdAt;
}
