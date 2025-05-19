package com.ktb.cafeboo.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record LoginResponse(
        String userId,
        String accessToken,
        boolean requiresOnboarding,
        @JsonIgnore String refreshToken
) {
    public LoginResponse withoutRefreshToken() {
        return new LoginResponse(userId, accessToken, requiresOnboarding, null);
    }
}
