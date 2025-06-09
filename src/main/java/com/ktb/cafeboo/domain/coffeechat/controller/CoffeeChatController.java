package com.ktb.cafeboo.domain.coffeechat.controller;


import com.ktb.cafeboo.domain.coffeechat.dto.JoinRoomRequest;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeechatMessage;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.coffeechat.service.ChatService;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor// HTTP 엔드포인트
@Slf4j
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
    public ResponseEntity<CoffeeChat> createChatRoom(@RequestParam String name) {
        CoffeeChat newRoom = chatRoomRepository.createChatRoom(name);
        return ResponseEntity.status(201).body(newRoom);
    }
}
