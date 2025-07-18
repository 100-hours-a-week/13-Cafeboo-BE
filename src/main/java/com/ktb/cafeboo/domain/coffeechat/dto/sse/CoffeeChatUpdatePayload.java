package com.ktb.cafeboo.domain.coffeechat.dto.sse;

public record CoffeeChatUpdatePayload(
        String coffeeChatId,
        Integer currentMemberCount
) {}
