package com.ktb.cafeboo.domain.user.dto;

public record UserProfileResponse(
        String nickname,
        String profileImageUrl,
        int dailyCaffeineLimitMg,
        int coffeeBean,
        int challengeCount
) {}
