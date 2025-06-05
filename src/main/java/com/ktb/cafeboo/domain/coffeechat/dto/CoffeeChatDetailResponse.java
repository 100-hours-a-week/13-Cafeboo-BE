package com.ktb.cafeboo.domain.coffeechat.dto;

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
        Location location,
        Writer writer,
        Boolean isJoined
) {
    public record Location(
            String address,
            double latitude,
            double longitude,
            String kakaoPlaceUrl
    ) {}

    public record Writer(
            String name,
            String profileImageUrl
    ) {}

    public static CoffeeChatDetailResponse from(CoffeeChat chat, Long userId) {

        return new CoffeeChatDetailResponse(
                chat.getId(),
                chat.getName(),
                chat.getContent(),
                chat.getMeetingTime().toLocalTime().toString(),
                chat.getMaxMemberCount(),
                chat.getCurrentMemberCount(),
                chat.getTagNames(),
                new Location(
                        chat.getAddress(),
                        chat.getLatitude().doubleValue(),
                        chat.getLongitude().doubleValue(),
                        chat.getKakaoPlaceUrl()
                ),
                new Writer(
                        chat.getWriter().getNickname(),
                        chat.getWriter().getProfileImageUrl()
                ),
                chat.isJoinedBy(userId)
        );
    }

}
