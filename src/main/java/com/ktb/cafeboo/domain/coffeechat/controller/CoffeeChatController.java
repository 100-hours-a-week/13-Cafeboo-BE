package com.ktb.cafeboo.domain.coffeechat.controller;


import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.coffeechat.service.ChatService;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.util.List;
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
    private final CoffeeChatRepository chatRoomRepository;
    private final ChatService chatService;

    // 모든 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<Collection<CoffeeChat>> getAllChatRooms() {
        return ResponseEntity.ok(chatRoomRepository.findAllRooms());
    }

    // 특정 채팅방 정보 조회
    @GetMapping("/{roomId}")
    public ResponseEntity<CoffeeChat> getChatRoomById(@PathVariable String roomId) {
        CoffeeChat room = chatRoomRepository.findRoomById(roomId);
        if (room != null) {
            return ResponseEntity.ok(room);
        }
        return ResponseEntity.notFound().build();
    }

    private final CoffeeChatService coffeeChatService;
    private final CoffeeChatMessageService coffeeChatMessageService;

    @PostMapping("/{roomId}/member")
    public ResponseEntity<String> joinCoffeechat(@PathVariable String roomId, @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        if (userDetails == null) {
            log.warn("인증되지 않은 사용자가 CoffeeChat {} 에 가입을 시도했습니다.", roomId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        Long userId = userDetails.getUserId();

        log.info("[CoffeeChatController.joinCoffeechat] - User {}가 CoffeeChat {}에 가입 시도.\n", userId, roomId);
        try {
            // ChatService의 startListeningToRoom() 호출
            chatService.startListeningToRoom(roomId, userId);
            log.info("User {} successfully started listening to room {}", userId, roomId);
            return ResponseEntity.ok("Successfully joined chat room " + roomId);
        } catch (Exception e) {
            log.error("Failed to join chat room {} for user {}: {}", roomId, userId, e.getMessage(), e);
            // 클라이언트에게 오류 응답 (HTTP)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to join chat room: " + e.getMessage());
        }
    }

    @GetMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Boolean> isUserJoinedRoom(@PathVariable String roomId, @PathVariable String userId) {
        boolean isJoined = chatService.isUserJoinedRoom(roomId, userId);
        log.info("Checking if user {} is joined to room {}: {}", userId, roomId, isJoined);
        return ResponseEntity.ok(isJoined);
    }

    // 특정 채팅방의 이전 메시지 이력 조회 (Redis Stream에서 가져옴)
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<CoffeechatMessage>> getChatRoomMessages(@PathVariable String roomId,
        @RequestParam(defaultValue = "+") String startId,
        @RequestParam(defaultValue = "100") long count) {
        log.info("[CoffeeChatController.getChatRoomMessages] - startId: {}, count: {}\n", startId, count);
        List<CoffeechatMessage> chatMessages = chatService.getChatRoomMessages(roomId, startId, count);

        return ResponseEntity.ok(chatMessages);
    }

    // 새로운 채팅방 생성 (예: POST /api/chatrooms?name=새로운방이름)
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
