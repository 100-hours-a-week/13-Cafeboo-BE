package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.dto.*;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatMemberService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatMessageService;
import com.ktb.cafeboo.domain.coffeechat.service.CoffeeChatService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/coffee-chats")
@RequiredArgsConstructor
public class CoffeeChatController {

    private final CoffeeChatService coffeeChatService;
    private final CoffeeChatMessageService coffeeChatMessageService;
    private final CoffeeChatMemberService coffeeChatMemberService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<ApiResponse<CoffeeChatCreateResponse>> createCoffeeChat(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CoffeeChatCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        log.info("[POST /api/v1/coffee-chats] userId: {} 커피챗 생성 요청 수신", userId);

        try {
            CoffeeChatCreateResponse response = coffeeChatService.create(userId, request);

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.COFFEECHAT_CREATE_SUCCESS, response));
        } catch (Exception e) {

            log.error("[POST /api/v1/coffee-chats] 커피챗 생성 및 참여 중 오류 발생: {}", e.getMessage(), e);

            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
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

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{coffeechatId}")
    public ResponseEntity<ApiResponse<CoffeeChatDetailResponse>> getCoffeeChatDetail(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId
    ) {
        CoffeeChatDetailResponse response = coffeeChatService.getDetail(coffeechatId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_LOAD_SUCCESS, response));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{coffeechatId}/members")
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

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{coffeechatId:\\d+}/members/{memberId:\\d+}")
    public ResponseEntity<Void> leaveChat(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId,
        @PathVariable Long memberId
    ) {
        coffeeChatService.leaveChat(coffeechatId, memberId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{coffeechatId:\\d+}")
    public ResponseEntity<Void> deleteCoffeeChat(
        @PathVariable Long coffeechatId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        log.info("[DELETE /api/v1/coffee-chats/{}] userId: {} 커피챗 삭제 요청 수신", coffeechatId, userId);

        coffeeChatService.delete(coffeechatId, userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @PreAuthorize("hasRole('USER')")
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

    @GetMapping("/{coffeechatId}/members")
    public ResponseEntity<ApiResponse<CoffeeChatMembersResponse>> getCoffeeChatMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long coffeechatId
    ) {
        Long userId = userDetails.getUserId();
        log.info("[GET /api/v1/coffee-chats/{}/members] userId: {} 커피챗 참여자 목록 조회 요청 수신", coffeechatId, userId);

        CoffeeChatMembersResponse response = coffeeChatService.getCoffeeChatMembers(coffeechatId);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.COFFEECHAT_MEMBER_LOAD_SUCCESS, response));
    }
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{coffeechatId}/membership")
    public ResponseEntity<ApiResponse<CoffeeChatMembershipCheckResponse>> checkMembership(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long coffeechatId
    ) {
        Long userId = userDetails.getUserId();
        log.info("[GET /api/v1/coffee-chats/{}/membership] userId: {} 커피챗 참여 여부 조회 요청 수신", coffeechatId, userId);

        CoffeeChatMembershipCheckResponse response = coffeeChatMemberService.checkMembership(coffeechatId, userId);

        return ResponseEntity
                .ok()
                .body(ApiResponse.of(SuccessStatus.COFFEECHAT_MEMBERSHIP_CHECK_SUCCESS, response));
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{coffeechatId}/listeners")
    public ResponseEntity<?> createCoffeeChatListener(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long coffeechatId
    ) {
        Long userId = userDetails.getUserId();
        log.info("[POST /api/v1/coffee-chats/{}/listeners] userId: {} 커피챗 리스너 생성 요청 수신", coffeechatId, userId);

        try {
            // chatService에서 Redis Stream Listener 활성화 로직 호출
            // startListeningToCoffeeChat 메서드가 idempotently 작동(이미 리스너가 있으면 생성하지 않음)하므로,
            // 매번 호출해도 안전합니다.
            coffeeChatService.startListeningToCoffeeChat(String.valueOf(coffeechatId));

            // 201 Created 응답 (리스너 리소스가 성공적으로 생성되었음을 의미)
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.COFFEECHAT_LISTENER_CREATED_SUCCESS, null));
        } catch (Exception e) {
            log.error("[POST /api/v1/coffee-chats/{}/listeners] 커피챗 리스너 생성 중 오류 발생: {}", coffeechatId, e.getMessage(), e);
            // 적절한 에러 상태 코드와 메시지로 응답
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }
}