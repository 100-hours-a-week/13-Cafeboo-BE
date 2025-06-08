package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.WriterDto;

import java.util.List;

public record CoffeeChatListResponse(
        String filter,
        List<CoffeeChatSummary> coffeechats
) {
    public record CoffeeChatSummary(
            Long coffechatId,
            String title,
            String time,
            int maxMemberCount,
            int currentMemberCount,
            List<String> tags,
            String address,
            WriterDto writer,

            // 조건부 필드
            Boolean isJoined,
            Boolean isReviewed
    ) {
        public static CoffeeChatSummary of(
                com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat chat,
                boolean isJoined,
                boolean isReviewed
        ) {
            return new CoffeeChatSummary(
                    chat.getId(),
                    chat.getName(),
                    chat.getMeetingTime().toLocalTime().toString(),
                    chat.getMaxMemberCount(),
                    chat.getCurrentMemberCount(),
                    chat.getTagNames(),
                    chat.getAddress(),
                    new WriterDto(
                            chat.getWriter().getNickname(),
                            chat.getWriter().getProfileImageUrl()
                    ),
                    isJoined,
                    isReviewed
            );
        }
    }
}

