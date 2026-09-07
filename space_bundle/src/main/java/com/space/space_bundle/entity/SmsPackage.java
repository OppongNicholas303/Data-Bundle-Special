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
@Document(collection = "sms_packages")
public class SmsPackage {

    @Id
    private String id;
    private String name;
    private int messagesCount;
    private BigDecimal price;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
