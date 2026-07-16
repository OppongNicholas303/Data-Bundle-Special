package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "result_checker_pricing")
public class ResultCheckerPricing {

    @Id
    private String serviceName; // e.g. VoucherPricePlatformWaecNew
    private BigDecimal amount; // Cost price from CheckerPort API
    
    private BigDecimal retailPrice; // Retail price set by the Admin

    private LocalDateTime updatedAt;
}
