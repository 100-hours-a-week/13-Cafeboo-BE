package com.ktb.cafeboo.domain.auth.dto;

public record GuestLoginResponse(
        String accessToken,
        String userId,
        String role
) {}
