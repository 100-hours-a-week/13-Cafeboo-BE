package com.ktb.cafeboo.domain.coffeechat.dto.common;

public record MemberDto(
        String memberId,
        String name,
        String profileImageUrl,
        boolean isHost
) {}