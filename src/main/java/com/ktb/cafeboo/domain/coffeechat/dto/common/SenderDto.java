package com.ktb.cafeboo.domain.coffeechat.dto.common;

public record SenderDto(
        Long memberId,
        String chatNickname,
        String profileImageUrl
) {}