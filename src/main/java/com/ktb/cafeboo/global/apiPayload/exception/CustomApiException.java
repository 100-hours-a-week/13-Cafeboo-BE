package com.ktb.cafeboo.global.apiPayload.exception;

import com.ktb.cafeboo.global.apiPayload.code.BaseCode;
import lombok.Getter;

@Getter
public class CustomApiException extends RuntimeException {
    private final BaseCode errorCode;

    public CustomApiException(BaseCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
