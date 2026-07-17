package com.space.space_bundle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoolreSmsResponse {
    private int status;
    private String code;
    private String message;
    private Object data;
    private Object go;
}
