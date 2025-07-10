package com.ktb.cafeboo.domain.coffeechat.dto;

public record CoffeeChatJoinResponse(
        String memberId,
        Integer currentMemberCount
) {
    public static CoffeeChatJoinResponse of(Long memberId, Integer currentMemberCount) {
        return new CoffeeChatJoinResponse(memberId.toString(), currentMemberCount);
    }
}