package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserCaffeineInfoResponse(
        int caffeineSensitivity,
        float averageDailyCaffeineIntake,
        String frequentDrinkTime,
        float dailyCaffeineLimitMg,
        float sleepSensitiveThresholdMg,
        List<String> userFavoriteDrinks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
