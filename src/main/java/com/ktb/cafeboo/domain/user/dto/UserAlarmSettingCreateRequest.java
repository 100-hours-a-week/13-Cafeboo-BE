package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserAlarmSettingCreateRequest {
    private boolean alarmWhenExceedIntake;
    private boolean alarmBeforeSleep;
    private boolean alarmForChat;
}

