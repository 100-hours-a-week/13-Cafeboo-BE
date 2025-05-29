package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserAlarmSettingCreateResponse(
        String userId,
        LocalDateTime createdAt
) {}
