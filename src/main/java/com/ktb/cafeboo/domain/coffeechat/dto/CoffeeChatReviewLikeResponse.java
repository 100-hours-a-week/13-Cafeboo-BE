package com.ktb.cafeboo.domain.coffeechat.dto;

public record CoffeeChatReviewLikeResponse(
        boolean liked,
        int likeCount
) {
}
