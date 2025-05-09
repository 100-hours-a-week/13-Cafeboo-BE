package com.ktb.cafeboo.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TokenRefreshResponse {
    private String userId;
    private String accessToken;
}