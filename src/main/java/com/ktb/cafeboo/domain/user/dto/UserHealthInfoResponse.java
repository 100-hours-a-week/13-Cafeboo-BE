package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserHealthInfoResponse(
        String gender,
        int age,
        float height,
        float weight,
        boolean isPregnant,
        boolean isTakingBirthPill,
        boolean isSmoking,
        boolean hasLiverDisease,
        String sleepTime,
        String wakeUpTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}