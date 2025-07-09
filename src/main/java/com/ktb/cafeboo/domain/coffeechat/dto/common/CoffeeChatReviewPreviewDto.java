package com.ktb.cafeboo.domain.coffeechat.dto.common;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;

import java.util.List;

public record CoffeeChatReviewPreviewDto(
        String coffeeChatId,
        String title,
        List<String> tags,
        String address,
        int likesCount,
        boolean liked,
        int imagesCount,
        String previewImageUrl
) {
    public static CoffeeChatReviewPreviewDto from(CoffeeChat coffeeChat, List<String> tagNames, int imagesCount, String previewImageUrl, boolean liked) {
        return new CoffeeChatReviewPreviewDto(
                coffeeChat.getId().toString(),
                coffeeChat.getName(),
                tagNames,
                coffeeChat.getAddress(),
                coffeeChat.getLikesCount(),
                liked,
                imagesCount,
                previewImageUrl
        );
    }
}
