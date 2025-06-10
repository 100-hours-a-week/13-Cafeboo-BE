package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.WriterDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;

import java.util.List;

public record CoffeeChatListResponse(
        String filter,
        List<CoffeeChatSummary> coffeechats
) {
    public record CoffeeChatSummary(
            String coffechatId,
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
            CoffeeChatMember writerMember = chat.getMembers().stream()
                    .filter(m -> m.getUser().getId().equals(chat.getWriter().getId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND));

            return new CoffeeChatSummary(
                    chat.getId().toString(),
                    chat.getName(),
                    chat.getMeetingTime().toLocalTime().toString(),
                    chat.getMaxMemberCount(),
                    chat.getCurrentMemberCount(),
                    chat.getTagNames(),
                    chat.getAddress(),
                    new WriterDto(
                            writerMember.getId().toString(),
                            writerMember.getChatNickname(),
                            writerMember.getProfileImageUrl()
                    ),
                    isJoined,
                    isReviewed
            );
        }
    }
}

