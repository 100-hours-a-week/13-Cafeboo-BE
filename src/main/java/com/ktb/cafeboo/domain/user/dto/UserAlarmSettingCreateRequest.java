package com.ktb.cafeboo.domain.user.dto;

public record UserAlarmSettingCreateRequest(
        boolean alarmWhenExceedIntake,
        boolean alarmBeforeSleep,
        boolean alarmForChat
) {}
