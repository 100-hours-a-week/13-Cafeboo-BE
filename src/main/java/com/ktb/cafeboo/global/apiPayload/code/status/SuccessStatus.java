package com.ktb.cafeboo.global.apiPayload.code.status;

import com.ktb.cafeboo.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseCode {

    OK(200, "SUCCESS", "요청이 성공적으로 처리되었습니다."),
    CREATED(201, "CREATED", "리소스가 성공적으로 생성되었습니다."),
    NO_CONTENT(204, "NO_CONTENT", "요청은 성공했지만 반환할 데이터가 없습니다."),

    // 유저 관련 응답
    EMAIL_DUPLICATION_CHECK_SUCCESS(200, "EMAIL_DUPLICATION_CHECK_SUCCESS", "이메일 중복 확인 결과를 반환했습니다.");

    private final int status;
    private final String code;
    private final String message;
}