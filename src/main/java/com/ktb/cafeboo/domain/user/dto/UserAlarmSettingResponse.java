package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserAlarmSettingResponse(
        boolean alarmWhenExceedIntake,
        boolean alarmBeforeSleep,
        boolean alarmForChat,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}