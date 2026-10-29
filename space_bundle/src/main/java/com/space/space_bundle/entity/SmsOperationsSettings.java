package com.space.space_bundle.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sms_operations_settings")
public class SmsOperationsSettings {
    @Id private String id;
    private boolean sendingEnabled;
    private boolean providerBundleReady;
    private String providerBundleBalanceNote;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
