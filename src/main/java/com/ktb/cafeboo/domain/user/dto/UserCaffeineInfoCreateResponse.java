package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class UserCaffeineInfoCreateResponse {
    private String userId;
    private LocalDateTime createdAt;
}

