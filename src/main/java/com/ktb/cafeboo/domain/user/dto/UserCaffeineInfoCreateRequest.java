package com.ktb.cafeboo.domain.user.dto;

import java.util.List;

public record UserCaffeineInfoCreateRequest(
        int caffeineSensitivity,
        float averageDailyCaffeineIntake,
        String frequentDrinkTime,
        List<String> userFavoriteDrinks
) {}
