package com.ktb.cafeboo.domain.user.dto;

public record UserAlarmSettingUpdateRequest(
        Boolean alarmWhenExceedIntake,
        Boolean alarmBeforeSleep,
        Boolean alarmForChat
) {}