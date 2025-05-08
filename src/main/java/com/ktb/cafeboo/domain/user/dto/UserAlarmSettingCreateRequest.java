package com.ktb.cafeboo.domain.user.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class UserAlarmSettingCreateRequest {
    private boolean alarmWhenExceedIntake;
    private boolean alarmBeforeSleep;
    private boolean alarmForChat;
}

