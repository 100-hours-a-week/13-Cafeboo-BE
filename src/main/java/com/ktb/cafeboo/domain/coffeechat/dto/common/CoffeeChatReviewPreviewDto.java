package com.ktb.cafeboo.domain.coffeechat.dto.common;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;

import java.util.List;

public record CoffeeChatReviewPreviewDto(
        String coffeeChatId,
        String title,
        List<String> tags,
        String address,
        int likesCount,
        int imagesCount,
        String previewImageUrl
) {
    public static CoffeeChatReviewPreviewDto from(CoffeeChat coffeeChat, int imagesCount, String previewImageUrl) {
        return new CoffeeChatReviewPreviewDto(
                coffeeChat.getId().toString(),
                coffeeChat.getName(),
                coffeeChat.getTagNames(),
                coffeeChat.getAddress(),
                coffeeChat.getLikesCount(),
                imagesCount,
                previewImageUrl
        );
    }
}
