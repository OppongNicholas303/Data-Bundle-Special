package com.space.space_bundle.dto.checkerport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckerPortCorrectionRequest {
    private String referenceId;
    private String indexNumber;
    private String dob;
    private String year;
    private String webhookCallbackUrl;
}
