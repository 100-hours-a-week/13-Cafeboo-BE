package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAlarmSettingResponse {
    private boolean alarmWhenExceedIntake;
    private boolean alarmBeforeSleep;
    private boolean alarmForChat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

