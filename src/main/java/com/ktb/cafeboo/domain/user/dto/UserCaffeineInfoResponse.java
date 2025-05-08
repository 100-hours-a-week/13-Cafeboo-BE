package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserCaffeineInfoResponse {
    private int caffeineSensitivity;
    private float averageDailyCaffeineIntake;
    private String frequentDrinkTime;
    private float dailyCaffeineLimitMg;
    private float sleepSensitiveThresholdMg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}