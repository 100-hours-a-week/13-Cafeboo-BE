package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserCaffeineInfoUpdateResponse(
        String userId,
        LocalDateTime updatedAt
) {}