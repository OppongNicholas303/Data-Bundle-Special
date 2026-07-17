package com.space.space_bundle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoolreSmsMessage {
    private String recipient;
    private String message;
    private String ref;
}
