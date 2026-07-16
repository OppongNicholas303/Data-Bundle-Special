package com.space.space_bundle.dto.checkerport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckerPortResponse<T> {
    private String status; // SUCCESS, FAILED, PENDING
    private String message;
    private T data;
    private String errorCode;
    private List<ErrorDetail> errors;
    private Map<String, Object> meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}
