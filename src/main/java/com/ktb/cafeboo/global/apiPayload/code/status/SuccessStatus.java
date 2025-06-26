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

    // 인증 관련 응답
    LOGIN_SUCCESS(200, "LOGIN_SUCCESS", "성공적으로 로그인되었습니다."),
    TOKEN_REFRESH_SUCCESS(200, "TOKEN_REFRESH_SUCCESS", "액세스 토큰이 성공적으로 재발급되었습니다."),

    // 유저 관련 응답
    EMAIL_DUPLICATION_CHECK_SUCCESS(200, "EMAIL_DUPLICATION_CHECK_SUCCESS", "이메일 중복 확인 결과를 반환했습니다."),
    BASIC_PROFILE_FETCH_SUCCESS(200, "BASIC_PROFILE_FETCH_SUCCESS", "기본 프로필이 성공적으로 조회되었습니다."),
    BASIC_PROFILE_UPDATE_SUCCESS(200, "BASIC_PROFILE_UPDATE_SUCCESS", "기본 프로필 정보가 성공적으로 수정되었습니다."),
    
    //유저 건강 프로필 관련 응답
    HEALTH_PROFILE_CREATION_SUCCESS(201, "HEALTH_PROFILE_CREATION_SUCCESS", "건강 프로필이 성공적으로 저장되었습니다."),
    HEALTH_PROFILE_UPDATE_SUCCESS(200, "HEALTH_PROFILE_UPDATE_SUCCESS", "건강 프로필이 성공적으로 수정되었습니다."),
    HEALTH_PROFILE_FETCH_SUCCESS(200, "HEALTH_PROFILE_FETCH_SUCCESS", "건강 프로필이 성공적으로 조회되었습니다."),

    //유저 카페인 프로필 관련 응답
    CAFFEINE_PROFILE_CREATION_SUCCESS(201, "CAFFEINE_PROFILE_CREATION_SUCCESS", "카페인 프로필이 성공적으로 저장되었습니다."),
    CAFFEINE_PROFILE_UPDATE_SUCCESS(200, "CAFFEINE_PROFILE_UPDATE_SUCCESS", "카페인 프로필이 성공적으로 수정되었습니다."),
    CAFFEINE_PROFILE_FETCH_SUCCESS(200, "CAFFEINE_PROFILE_FETCH_SUCCESS", "카페인 프로필이 성공적으로 조회되었습니다."),

    // 유저 알림 설정 관련 응답
    ALARM_SETTING_CREATION_SUCCESS(201, "ALARM_SETTING_CREATION_SUCCESS", "알람 설정이 성공적으로 저장되었습니다."),
    ALARM_SETTING_UPDATE_SUCCESS(200, "ALARM_SETTING_UPDATE_SUCCESS", "알람 설정이 성공적으로 수정되었습니다."),
    ALARM_SETTING_FETCH_SUCCESS(200, "ALARM_SETTING_FETCH_SUCCESS", "알람 설정 정보가 성공적으로 조회되었습니다."),

    //카페인 섭취 관련 응답
    CAFFEINE_INTAKE_RECORDED(201, "CAFFEINE_INTAKE_RECORDED", "카페인 섭취 내역이 성공적으로 등록되었습니다."),
    CAFFEINE_INTAKE_UPDATED(200, "CAFFEINE_INTAKE_UPDATED", "카페인 섭취 내역이 성공적으로 수정되었습니다."),
    CAFFEINE_INTAKE_DELETED(204, "CAFFEINE_INTAKE_DELETED", "카페인 섭취 내역이 성공적으로 삭제되었습니다."),

    //카페인 리포트 관련 응답
    DAILY_CAFFEINE_REPORT_SUCCESS(200, "DAILY_CAFFEINE_REPORT_SUCCESS", "일일 카페인 섭취 리포트를 성공적으로 조회했습니다."),
    WEEKLY_CAFFEINE_REPORT_SUCCESS(200, "WEEKLY_CAFFEINE_REPORT_SUCCESS", "주간 카페인 섭취 리포트를 성공적으로 조회했습니다."),
    MONTHLY_CAFFEINE_REPORT_SUCCESS(200, "MONTHLY_CAFFEINE_REPORT_SUCCESS", "월간 카페인 섭취 리포트를 성공적으로 조회했습니다."),
    YEARLY_CAFFEINE_REPORT_SUCCESS(200, "YEARLY_CAFFEINE_REPORT_SUCCESS", "연간 카페인 섭취 리포트를 성공적으로 조회했습니다."),
  
    //카페인 다이어리 조회 관련 응답
    MONTHLY_CAFFEINE_CALENDAR_SUCCESS(200, "MONTHLY_CAFFEINE_CALENDAR_SUCCESS", "다이어리 월간 카페인 섭취 기록이 정상적으로 조회되었습니다."),
    DAILY_CAFFEINE_CALENDAR_SUCCESS(200, "DAILY_CAFFEINE_CALENDAR_SUCCESS", "다이어리 일일 카페인 섭취 기록이 정상적으로 조회되었습니다."),

    //AI 리포트 생성 관련 응답
    REPORT_GENERATION_SUCCESS(200, "AI_GENERATION_REPORT", "AI 리포트 생성 요청 성공"),
    AI_CALLBACK_SUCCESS(200, "AI_CALLBACK_SUCCESS", "AI 콜백 함수 호출 성공"),

    //커피챗 관련 응답
    COFFEECHAT_CREATE_SUCCESS(201, "COFFEECHAT_CREATE_SUCCESS", "커피챗이 성공적으로 생성되었습니다."),
    COFFEECHAT_LIST_LOAD_SUCCESS(200, "COFFEECHAT_LIST_LOAD_SUCCESS", "커피챗 목록을 성공적으로 조회했습니다."),
    COFFEECHAT_LOAD_SUCCESS(200, "COFFEECHAT_LOAD_SUCCESS", "커피챗 상세내용을 성공적으로 조회했습니다."),
    COFFEECHAT_JOIN_SUCCESS(201, "COFFEECHAT_JOIN_SUCCESS", "커피챗에 성공적으로 참여하였습니다."),
    COFFEECHAT_MESSAGES_LOAD_SUCCESS(200, "COFFEECHAT_MESSAGES_LOAD_SUCCESS", "커피챗 메시지를 성공적으로 조회했습니다."),
    COFFEECHAT_MEMBER_LOAD_SUCCESS(200, "COFFEECHAT_MEMBER_LOAD_SUCCESS", "커피챗 참여자 목록을 성공적으로 조회했습니다."),
    COFFEECHAT_LISTENER_CREATED_SUCCESS(201, "COFFEECHAT_LISTENER_CREATED_SUCCESS", "커피챗 리스너가 성공적으로 생성되었습니다"),

    // 커피챗 후기

    COFFEECHAT_REVIEW_CREATE_SUCCESS(201, "COFFECHAT_REVIEW_CREATE_SUCCESS", "커피챗 후기가 성공적으로 등록되었습니다."),
    COFFEECHAT_REVIEW_LIST_LOAD_SUCCESS(200, "COFFEECHAT_REVIEW_LIST_LOAD_SUCCESS", "커피챗 후기 목록을 성공적으로 조회했습니다."),
    COFFEECHAT_REVIEW_LOAD_SUCCESS(200, "COFFEECHAT_REVIEW_LOAD_SUCCESS", "커피챗 후기를 성공적으로 조회했습니다."),
    COFFEECHAT_LIKE_SUCCESS(200, "COFFEECHAT_LIKE_SUCCESS", "커피챗 후기를 좋아요 처리했습니다."),
    COFFEECHAT_UNLIKE_SUCCESS(200, "COFFEECHAT_UNLIKE_SUCCESS", "커피챗 후기 좋아요를 취소했습니다."),
    COFFEECHAT_MEMBERSHIP_CHECK_SUCCESS(200, "COFFEECHAT_MEMBERSHIP_CHECK_SUCCESS", "커피챗 참여 여부를 성공적으로 조회했습니다.")
    ;


    private final int status;
    private final String code;
    private final String message;
}