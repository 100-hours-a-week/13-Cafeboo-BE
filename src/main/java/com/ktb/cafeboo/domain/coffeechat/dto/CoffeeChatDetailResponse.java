package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.WriterDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;

import java.util.List;

public record CoffeeChatDetailResponse(
        Long coffechatId,
        String title,
        String content,
        String time,
        int maxMemberCount,
        int currentMemberCount,
        List<String> tags,
        LocationDto location,
        WriterDto writer,
        Boolean isJoined
) {
    public static CoffeeChatDetailResponse from(CoffeeChat chat, Long userId) {

        return new CoffeeChatDetailResponse(
                chat.getId(),
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
                new WriterDto(
                        chat.getWriter().getNickname(),
                        chat.getWriter().getProfileImageUrl()
                ),
                chat.isJoinedBy(userId)
        );
    }
}
