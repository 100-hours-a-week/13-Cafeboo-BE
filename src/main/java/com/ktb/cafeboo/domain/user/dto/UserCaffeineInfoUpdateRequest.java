package com.ktb.cafeboo.domain.user.dto;

import java.util.List;

public record UserCaffeineInfoUpdateRequest(
        Integer caffeineSensitivity,
        Float averageDailyCaffeineIntake,
        String frequentDrinkTime,
        List<String> userFavoriteDrinks
) {}
