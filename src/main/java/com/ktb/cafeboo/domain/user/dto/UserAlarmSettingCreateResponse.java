package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserAlarmSettingCreateResponse {
    private String userId;
    private LocalDateTime createdAt;
}

