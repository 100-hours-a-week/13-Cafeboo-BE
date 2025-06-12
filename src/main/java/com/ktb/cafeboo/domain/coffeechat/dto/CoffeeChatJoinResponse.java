package com.ktb.cafeboo.domain.coffeechat.dto;

public record CoffeeChatJoinResponse(
        String memberId
) {
    public static CoffeeChatJoinResponse from(Long memberId) {
        return new CoffeeChatJoinResponse(memberId.toString());
    }
}
