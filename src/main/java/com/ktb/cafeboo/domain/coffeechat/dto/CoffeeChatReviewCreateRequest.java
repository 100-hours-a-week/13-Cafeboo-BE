package com.ktb.cafeboo.domain.coffeechat.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CoffeeChatReviewCreateRequest(
        String memberId,
        String text,
        List<MultipartFile> images
) {}