package com.ktb.cafeboo.domain.coffeechat.dto.common;

public record SenderDto(
        String userId,
        String nickname,
        String profileImageUrl
) {}