package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.dto.*;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatMessageService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/coffee-chats")
@RequiredArgsConstructor
public class CoffeeChatController {

    private final CoffeeChatService coffeeChatService;
    private final CoffeeChatMessageService coffeeChatMessageService;

    @PostMapping
    public ResponseEntity<ApiResponse<CoffeeChatCreateResponse>> createCoffeeChat(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CoffeeChatCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        log.info("[POST /api/v1/coffee-chats] userId: {} 커피챗 생성 요청 수신", userId);

        CoffeeChatCreateResponse response = coffeeChatService.create(userId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.of(SuccessStatus.COFFEECHAT_CREATE_SUCCESS, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CoffeeChatListResponse>> getCoffeeChatList(
        @RequestParam("status") String status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        log.info("[GET /api/v1/coffee-chats?status={}] 커피챗 목록 조회 요청 수신 - userId: {}", status, userId);

        CoffeeChatListResponse response = coffeeChatService.getCoffeeChatsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_LIST_LOAD_SUCCESS, response));
    }

    @GetMapping("/{coffeechatId}")
    public ResponseEntity<ApiResponse<CoffeeChatDetailResponse>> getCoffeeChatDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId
    ) {
        CoffeeChatDetailResponse response = coffeeChatService.getDetail(coffeechatId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_LOAD_SUCCESS, response));
    }

    @PostMapping("/{coffeechatId}/member")
    public ResponseEntity<ApiResponse<CoffeeChatJoinResponse>> joinCoffeeChat(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId,
        @RequestBody CoffeeChatJoinRequest request
    ) {
        CoffeeChatJoinResponse response = coffeeChatService.join(
            userDetails.getUserId(),
            coffeechatId,
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.of(SuccessStatus.COFFEECHAT_JOIN_SUCCESS, response));
    }

    @DeleteMapping("/{coffeechatId}/member/{memberId}")
    public ResponseEntity<Void> leaveChat(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId,
        @PathVariable Long memberId
    ) {
        coffeeChatService.leaveChat(coffeechatId, memberId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{coffeechatId}")
    public ResponseEntity<Void> deleteCoffeeChat(
        @PathVariable Long coffeechatId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        log.info("[DELETE /api/v1/coffee-chats/{}] userId: {} 커피챗 삭제 요청 수신", coffeechatId, userId);

        coffeeChatService.delete(coffeechatId, userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/{coffeechatId}/messages")
    public ResponseEntity<ApiResponse<CoffeeChatMessagesResponse>> getMessages(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId,
        @RequestParam String cursor,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "desc") String order
    ) {
        log.info("[CoffeeChatMessageController.getMessages] 메시지 조회 요청 - userId={}, coffeechatId={}, cursor={}, limit={}, order={}",
            userDetails.getUserId(), coffeechatId, cursor, limit, order);

        CoffeeChatMessagesResponse response = coffeeChatMessageService.getMessages(
            userDetails.getUserId(),
            coffeechatId,
            cursor,
            limit,
            order
        );

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_MESSAGES_LOAD_SUCCESS, response));
    }
}