package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserHealthInfoUpdateResponse {
    private String userId;
    private LocalDateTime updatedAt;
}
