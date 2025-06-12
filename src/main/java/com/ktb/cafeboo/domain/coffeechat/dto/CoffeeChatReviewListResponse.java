package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.CoffeeChatReviewPreviewDto;

import java.util.List;

public record CoffeeChatReviewListResponse(
        String filter,
        int totalReviewCount,
        List<CoffeeChatReviewPreviewDto> coffeeChatReviews
) {
}
