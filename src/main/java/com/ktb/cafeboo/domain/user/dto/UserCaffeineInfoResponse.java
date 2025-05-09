package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserCaffeineInfoResponse {
    private int caffeineSensitivity;
    private float averageDailyCaffeineIntake;
    private String frequentDrinkTime;
    private float dailyCaffeineLimitMg;
    private float sleepSensitiveThresholdMg;
    private List<String> userFavoriteDrinks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}