package com.ktb.cafeboo.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String userId;
    private String accessToken;
    private boolean requiresOnboarding;

    @JsonIgnore
    private String refreshToken;

    public LoginResponse withoutRefreshToken() {
        return LoginResponse.builder()
                .userId(this.userId)
                .accessToken(this.accessToken)
                .requiresOnboarding(this.requiresOnboarding)
                .build();
    }
}
