package com.ktb.cafeboo.global.apiPayload.code.status;

import com.ktb.cafeboo.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseCode {

    // 기본 오류
    BAD_REQUEST(400, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(404, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),

    // auth 도메인 관련 오류
    UNSUPPORTED_SOCIAL_LOGIN_TYPE(400, "UNSUPPORTED_SOCIAL_LOGIN_TYPE", "지원하지 않는 소셜 로그인 타입입니다.");


    private final int status;
    private final String code;
    private final String message;
}