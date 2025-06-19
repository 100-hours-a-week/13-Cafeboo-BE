package com.ktb.cafeboo.domain.coffeechat.dto;

import lombok.Builder;

@Builder
public record CoffeeChatMembershipCheckResponse(
        boolean isMember,
        String userId,
        String memberId,
        String chatNickname
) {}
