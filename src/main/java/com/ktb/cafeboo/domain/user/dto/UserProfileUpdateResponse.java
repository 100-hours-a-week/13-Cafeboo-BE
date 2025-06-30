package com.ktb.cafeboo.domain.user.dto;

import java.time.LocalDateTime;

public record UserProfileUpdateResponse(
        String userId,
        LocalDateTime updatedAt
) {}