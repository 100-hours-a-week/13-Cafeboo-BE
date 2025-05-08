package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserCaffeineInfoCreateResponse {
    private Long userId;
    private LocalDateTime createdAt;
}

