package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewListResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewResponse;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatReviewService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        log.info("[GET /coffee-chats/reviews] 후기 리스트 조회 요청 - userId: {}, status: {}", userId, status);

        CoffeeChatReviewListResponse response = coffeeChatReviewService.getCoffeeChatReviewsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_REVIEW_LIST_LOAD_SUCCESS, response));
    }

    @GetMapping("/{coffeeChatId}")
    public ResponseEntity<ApiResponse<CoffeeChatReviewResponse>> getCoffeeChatReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long coffeeChatId
    ) {
        Long userId = userDetails.getUserId();
        log.info("[GET /coffee-chats/reviews/{}] 후기 상세 조회 요청 - userId: {}", coffeeChatId, userId);

        CoffeeChatReviewResponse response = coffeeChatReviewService.getReviewByCoffeeChatId(coffeeChatId);

        return ResponseEntity.ok(
                ApiResponse.of(SuccessStatus.COFFEECHAT_REVIEW_LOAD_SUCCESS, response)
        );
    }
}
