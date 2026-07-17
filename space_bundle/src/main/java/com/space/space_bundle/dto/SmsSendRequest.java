package com.space.space_bundle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SmsSendRequest {
    @NotBlank(message = "Sender ID is required")
    private String senderId;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    @NotEmpty(message = "At least one recipient is required")
    private List<String> recipients;
}
