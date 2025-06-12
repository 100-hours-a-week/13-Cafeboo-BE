package com.ktb.cafeboo.domain.coffeechat.dto.common;

import java.util.List;

public record ReviewDto(
        String reviewId,
        String text,
        List<String> imageUrls,
        MemberDto writer
) {}
