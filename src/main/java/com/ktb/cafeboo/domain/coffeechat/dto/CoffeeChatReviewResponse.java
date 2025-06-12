package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.ReviewDto;

import java.util.List;

public record CoffeeChatReviewResponse(
        String coffeeChatId,
        String title,
        String time,
        List<String> tags,
        String address,
        int likeCount,
        List<ReviewDto> reviews
) {}
