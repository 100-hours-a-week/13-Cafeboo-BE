package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserAlarmSettingUpdateResponse(
        String userId,
        LocalDateTime updatedAt
) {}