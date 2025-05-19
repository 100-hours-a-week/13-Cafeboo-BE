package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserHealthInfoUpdateResponse(
        String userId,
        LocalDateTime updatedAt
) {}