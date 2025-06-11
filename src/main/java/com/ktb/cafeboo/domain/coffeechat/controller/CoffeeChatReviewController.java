package com.ktb.cafeboo.domain.review.controller;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewListResponse;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatReviewService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coffee-chats/reviews")
@RequiredArgsConstructor
public class CoffeeChatReviewController {

    private final CoffeeChatReviewService coffeeChatReviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<CoffeeChatReviewListResponse>> getReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "status", defaultValue = "all") String status
    ) {
        Long userId = userDetails.getUserId();
        CoffeeChatReviewListResponse response = coffeeChatReviewService.getCoffeeChatReviewsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_REVIEW_LIST_LOAD_SUCCESS, response));
    }
}
