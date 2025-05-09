package com.ktb.cafeboo.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class KakaoLoginResponse {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private boolean requiresOnboarding;
}
