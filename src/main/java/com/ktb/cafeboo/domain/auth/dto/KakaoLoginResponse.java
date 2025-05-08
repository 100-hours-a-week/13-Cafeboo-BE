package com.ktb.cafeboo.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoLoginResponse {
    private String accessToken;
    private String refreshToken;
    private boolean requiresOnboarding;
}
