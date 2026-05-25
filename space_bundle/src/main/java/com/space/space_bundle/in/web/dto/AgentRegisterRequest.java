package com.space.space_bundle.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRegisterRequest {
    @NotBlank
    private String businessName;
}
