package com.ktb.cafeboo.domain.coffeechat.dto;

public record CoffeeChatJoinResponse(
        Long memberId
) {
    public static CoffeeChatJoinResponse from(Long memberId) {
        return new CoffeeChatJoinResponse(memberId);
    }
}
