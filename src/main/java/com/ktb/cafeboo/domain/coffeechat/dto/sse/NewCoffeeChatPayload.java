package com.ktb.cafeboo.domain.coffeechat.dto.sse;

import java.util.List;

public record NewCoffeeChatPayload(
        String coffeeChatId,
        String title,
        String date,
        String time,
        Integer maxMemberCount,
        Integer currentMemberCount,
        List<String> tags,
        String address,
        Writer writer
) {
    public record Writer(
            Long memberId,
            String chatNickname,
            String profileImageUrl,
            boolean isHost
    ) {}
}
