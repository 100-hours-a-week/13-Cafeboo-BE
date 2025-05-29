package com.ktb.cafeboo.domain.user.dto;

public record UserProfileResponse(
        String nickname,
        int dailyCaffeineLimitMg,
        int coffeeBean,
        int challengeCount
) {}
