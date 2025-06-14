package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateRequest;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewLikeResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewListResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewResponse;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatLikeService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatReviewService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/coffee-chats/reviews")
@RequiredArgsConstructor
public class CoffeeChatReviewController {

    private final CoffeeChatReviewService coffeeChatReviewService;
    private final CoffeeChatLikeService coffeeChatLikeService;

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

    @PostMapping(value = "/{coffeechatId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CoffeeChatReviewCreateResponse>> createCoffeeChatReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long coffeechatId,
            @RequestPart("memberId") String memberId,
            @RequestPart("text") String text,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        Long userId = userDetails.getUserId();
        log.info("[POST /api/v1/coffee-chats/reviews/{}] userId: {} 커피챗 후기 작성 요청 수신", coffeechatId, userId);

        CoffeeChatReviewCreateRequest request = new CoffeeChatReviewCreateRequest(memberId, text, images);

        CoffeeChatReviewCreateResponse response = coffeeChatReviewService.createCoffeeChatReview(
                userId,
                coffeechatId,
                request
        );

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_REVIEW_CREATE_SUCCESS, response));
    }

    @PostMapping("/{coffeeChatId}/likes")
    public ResponseEntity<ApiResponse<CoffeeChatReviewLikeResponse>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long coffeeChatId
    ) {
        Long userId = userDetails.getUserId();
        log.info("[POST /api/v1/coffee-chats/reviews/{}/likes] 좋아요 토글 요청 - userId: {}", coffeeChatId, userId);

        CoffeeChatReviewLikeResponse response = coffeeChatLikeService.toggleLike(userId, coffeeChatId);

        SuccessStatus resultStatus = response.liked()
                ? SuccessStatus.COFFEECHAT_LIKE_SUCCESS
                : SuccessStatus.COFFEECHAT_UNLIKE_SUCCESS;

        return ResponseEntity.ok(ApiResponse.of(resultStatus, response));
    }
}
