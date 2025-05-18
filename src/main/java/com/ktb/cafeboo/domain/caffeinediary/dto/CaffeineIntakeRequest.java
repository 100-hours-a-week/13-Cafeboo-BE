package com.ktb.cafeboo.domain.caffeinediary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CaffeineIntakeRequest(
        String drinkId,
        LocalDateTime intakeTime,
        Integer drinkCount,
        Float caffeineAmount,
        String drinkSize
) {}
