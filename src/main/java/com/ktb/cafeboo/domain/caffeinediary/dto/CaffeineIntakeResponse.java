package com.ktb.cafeboo.domain.caffeinediary.dto;

import java.time.LocalDateTime;

public record CaffeineIntakeResponse(
        String id,
        String drinkId,
        String drinkName,
        LocalDateTime intakeTime,
        Integer drinkCount,
        Float caffeineAmount
) {}
