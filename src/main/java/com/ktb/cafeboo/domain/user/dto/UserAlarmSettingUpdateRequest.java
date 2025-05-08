package com.ktb.cafeboo.domain.user.dto;

import lombok.Getter;

@Getter
public class UserAlarmSettingUpdateRequest {
    private Boolean alarmWhenExceedIntake;
    private Boolean alarmBeforeSleep;
    private Boolean alarmForChat;
}

