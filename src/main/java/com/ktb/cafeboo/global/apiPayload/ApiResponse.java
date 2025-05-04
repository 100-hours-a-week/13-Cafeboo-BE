package com.ktb.cafeboo.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ktb.cafeboo.global.apiPayload.code.BaseCode;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
@JsonPropertyOrder({"status", "code", "message", "data"})
public class ApiResponse<T> {
    private int status;
    private String code;
    private String message;
    private T data;

    // 성공 응답
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(
                SuccessStatus.OK.getStatus(),
                SuccessStatus.OK.getCode(),
                SuccessStatus.OK.getMessage(),
                result
        );
    }

    public static <T> ApiResponse<T> of(BaseCode code, T result) {
        return new ApiResponse<>(
                code.getStatus(),
                code.getCode(),
                code.getMessage(),
                result
        );
    }

    // 실패 응답
    public static <T> ApiResponse<T> onFailure(BaseCode errorCode) {
        return new ApiResponse<>(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}
