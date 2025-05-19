package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserHealthInfoCreateResponse(
        String userId,
        LocalDateTime createdAt
) {}
