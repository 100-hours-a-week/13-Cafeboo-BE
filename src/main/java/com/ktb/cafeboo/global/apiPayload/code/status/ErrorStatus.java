package com.ktb.cafeboo.global.apiPayload.code.status;

import com.ktb.cafeboo.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements BaseCode {

    // 기본 오류
    BAD_REQUEST(400, "BAD_REQUEST", "요청 형식 또는 값이 올바르지 않습니다."),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(404, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_ERROR", "요청을 처리하는 도중 서버에서 문제가 발생했습니다."),

    // 인증 관련 오류
    UNSUPPORTED_SOCIAL_LOGIN_TYPE(400, "UNSUPPORTED_SOCIAL_LOGIN_TYPE", "지원하지 않는 소셜 로그인 타입입니다."),
    ACCESS_TOKEN_INVALID(401, "ACCESS_TOKEN_INVALID", "유효한 인증 정보가 필요합니다. 토큰을 확인해주세요."),
    ACCESS_TOKEN_EXPIRED(401, "ACCESS_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다. 토큰을 재발급 받아주세요."),
    REFRESH_TOKEN_INVALID(401, "REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(401, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_MISMATCH(401, "REFRESH_TOKEN_MISMATCH", "리프레시 토큰이 일치하지 않습니다."),

    // 유저 관련 오류
    USER_NOT_FOUND(401, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    ACCESS_DENIED(403, "ACCESS_DENIED", "해당 요청에 대한 권한이 없습니다."),

    HEALTH_PROFILE_ALREADY_EXISTS(409, "HEALTH_PROFILE_ALREADY_EXISTS", "이미 건강 정보가 등록되어 있습니다."),
    HEALTH_PROFILE_NOT_FOUND(404, "HEALTH_PROFILE_NOT_FOUND", "건강 정보가 존재하지 않습니다."),

    CAFFEINE_PROFILE_ALREADY_EXISTS(409, "CAFFEINE_PROFILE_ALREADY_EXISTS", "이미 카페인 정보가 등록되어 있습니다."),
    CAFFEINE_PROFILE_NOT_FOUND(404, "CAFFEINE_PROFILE_NOT_FOUND", "카페인 정보가 존재하지 않습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}