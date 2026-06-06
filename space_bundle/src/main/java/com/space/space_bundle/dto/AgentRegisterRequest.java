package com.space.space_bundle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRegisterRequest {
    @NotBlank private String businessName;
}
