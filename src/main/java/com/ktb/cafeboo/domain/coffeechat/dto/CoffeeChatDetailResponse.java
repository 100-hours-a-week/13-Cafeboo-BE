package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MemberDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;

import java.util.List;

public record CoffeeChatDetailResponse(
        String coffeChatId,
        String title,
        String content,
        String time,
        int maxMemberCount,
        int currentMemberCount,
        List<String> tags,
        LocationDto location,
        MemberDto writer,
        Boolean isJoined
) {
    public static CoffeeChatDetailResponse from(CoffeeChat chat, Long userId) {
        CoffeeChatMember writerMember = chat.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(chat.getWriter().getId()))
                .findFirst()
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND));

        return new CoffeeChatDetailResponse(
                chat.getId().toString(),
                chat.getName(),
                chat.getContent(),
                chat.getMeetingTime().toLocalTime().toString(),
                chat.getMaxMemberCount(),
                chat.getCurrentMemberCount(),
                chat.getTagNames(),
                new LocationDto(
                        chat.getAddress(),
                        chat.getLatitude(),
                        chat.getLongitude(),
                        chat.getKakaoPlaceUrl()
                ),
                new MemberDto(
                        writerMember.getId().toString(),
                        writerMember.getChatNickname(),
                        writerMember.getProfileImageUrl(),
                        writerMember.isHost()
                ),
                chat.isJoinedBy(userId)
        );
    }
}
