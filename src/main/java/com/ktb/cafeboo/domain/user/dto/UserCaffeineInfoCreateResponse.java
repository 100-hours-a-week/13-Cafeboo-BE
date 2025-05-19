package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserCaffeineInfoCreateResponse(
        String userId,
        LocalDateTime createdAt
) {}