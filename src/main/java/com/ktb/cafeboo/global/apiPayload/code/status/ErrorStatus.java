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

    // 인증 관련 오류 (서버 자체)
    UNSUPPORTED_SOCIAL_LOGIN_TYPE(400, "UNSUPPORTED_SOCIAL_LOGIN_TYPE", "지원하지 않는 소셜 로그인 타입입니다."),
    ACCESS_TOKEN_INVALID(401, "ACCESS_TOKEN_INVALID", "유효한 인증 정보가 필요합니다. 토큰을 확인해주세요."),
    ACCESS_TOKEN_EXPIRED(401, "ACCESS_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다. 토큰을 재발급 받아주세요."),
    ACCESS_TOKEN_BLACKLISTED(401, "ACCESS_TOKEN_BLACKLISTED", "해당 토큰은 더 이상 유효하지 않습니다. 다시 로그인해 주세요."),
    REFRESH_TOKEN_INVALID(401, "REFRESH_TOKEN_INVALID", "리프레시 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(401, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_MISMATCH(401, "REFRESH_TOKEN_MISMATCH", "리프레시 토큰이 일치하지 않습니다."),

    // 인증 관련 오류 (OAuth)
    OAUTH_TOKEN_NOT_FOUND(404, "OAUTH_TOKEN_NOT_FOUND", "해당 유저의 OAuth 토큰 정보가 존재하지 않습니다."),
    KAKAO_TOKEN_REFRESH_FAILED(500, "KAKAO_TOKEN_REFRESH_FAILED", "카카오 토큰 갱신에 실패했습니다. 다시 로그인해주세요."),

    // 유저 관련 오류
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    ACCESS_DENIED(403, "ACCESS_DENIED", "해당 요청에 대한 권한이 없습니다."),

    HEALTH_PROFILE_ALREADY_EXISTS(409, "HEALTH_PROFILE_ALREADY_EXISTS", "이미 건강 정보가 등록되어 있습니다."),
    HEALTH_PROFILE_NOT_FOUND(404, "HEALTH_PROFILE_NOT_FOUND", "건강 정보가 존재하지 않습니다."),

    CAFFEINE_PROFILE_ALREADY_EXISTS(409, "CAFFEINE_PROFILE_ALREADY_EXISTS", "이미 카페인 정보가 등록되어 있습니다."),
    CAFFEINE_PROFILE_NOT_FOUND(404, "CAFFEINE_PROFILE_NOT_FOUND", "카페인 정보가 존재하지 않습니다."),

    ALARM_SETTING_ALREADY_EXISTS(409, "ALARM_SETTING_ALREADY_EXISTS", "이미 알림 설정이 존재합니다."),
    ALARM_SETTING_NOT_FOUND(404, "ALARM_SETTING_NOT_FOUND", "알림 설정 정보를 찾을 수 없습니다."),

    // 섭취 내역 관련 오류
    INTAKE_INFO_NOT_FOUND(404, "INTAKE_INFO_NOT_FOUND", "섭취 내역을 찾을 수 없습니다."),

    // 음료 관련 오류
    DRINK_NOT_FOUND(404, "DRINK_NOT_FOUND", "음료 정보를 찾을 수 없습니다."),
    DRINK_SIZE_NOT_FOUND(404, "DRINK_SIZE_NOT_FOUND", "음료 정보를 찾을 수 없습니다."),

    // 외부 시스템 관련 오류
    AI_SERVER_ERROR(502, "AI_SERVER_ERROR", "AI 서버와의 통신 중 오류가 발생했습니다."),
    S3_REVIEW_IMAGE_UPLOAD_FAILED(500, "S3_REVIEW_IMAGE_UPLOAD_FAILED", "S3에 후기 이미지를 업로드하는 데 실패했습니다."),

    //리포트 관련 오류
    REPORT_NOT_FOUND(404, "REPORT_NOT_FOUND", "리포트 정보를 찾을 수 없습니다."),

    //AI 리포트 생성 관련 오류
    REPORT_GENERATION_FAILED(500, "REPORT_GENERATION_FAILED", "AI 리포트 생성에 실패했습니다"),

    //파라미터 관련 오류
    INVALID_PARAMETER(400, "INVALID_PARAMETER", "요청 파라미터의 올바르지 않습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}