package com.ktb.cafeboo.domain.coffeechat.dto.common;

public record MemberDto(
        String memberId,
        String chatNickname,
        String profileImageUrl,
        boolean isHost
) {}