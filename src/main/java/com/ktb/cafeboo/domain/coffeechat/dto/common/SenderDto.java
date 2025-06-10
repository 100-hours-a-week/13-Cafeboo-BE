package com.ktb.cafeboo.domain.coffeechat.dto.common;

public record SenderDto(
        String memberId,
        String chatNickname,
        String profileImageUrl
) {}