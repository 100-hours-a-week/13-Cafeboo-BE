package com.ktb.cafeboo.domain.coffeechat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ktb.cafeboo.domain.coffeechat.dto.JoinRoomRequest;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeechatMessage;
import com.ktb.cafeboo.domain.coffeechat.service.ChatService;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate; // 메시지 브로커로 메시지를 라우팅하는 역할
    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate 주입
    private StreamOperations<String, Object, Object> streamOperations; // StreamOperations 선언

    private final ChatService chatService;

    @PostConstruct
    private void init() {
        this.streamOperations = redisTemplate.opsForStream();
    }

    // 클라이언트에서 /chatrooms/{roomId}로 메시지를 보내면 이 메서드가 처리
    @MessageMapping("/chatrooms/{roomId}")
    public void handleCoffeeChatMessage(@DestinationVariable String roomId, @Payload CoffeechatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 클라이언트에서 보낸 메시지 로그
        log.info("[ChatController.handleCoffeeChatMessage] - handleCoffeeChatMessage 호출");

        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            log.warn("Received message for room {} without a valid session ID. Message content: {}", roomId, chatMessage.getContent());
            return; // 유효한 세션 ID가 없으면 STOMP 연결이 활성화되지 않을 것이기에 메시지 처리 중단
        }

        try{
            chatService.handleNewMessage(roomId, chatMessage);
        }
        catch (JsonProcessingException e){
            log.error("[ChatController.handleCoffeeChatMessage] - 메시지 직렬화 실패 오류. roomId: {}, {}", roomId, e.getMessage());
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors", "메시지 전송 실패: 유효하지 않은 메시지 형태.");
        }
        catch (Exception e) {
            log.error("[ChatController.handleCoffeeChatMessage] - 메시지 전송 실패");
            messagingTemplate.convertAndSendToUser(headerAccessor.getSessionId(), "/queue/errors", "메시지 전송 실패: 서버 오류.");
        }

//before, 작동 되는 거 확인하면 삭제
//        String coffeeChatStreamKey = "coffeechat:" + roomId + ":stream";
//
//        // ChatMessage 객체를 Map<String, String>으로 변환
//        Map<String, String> messageMap = new HashMap<>();
//        messageMap.put("userId", chatMessage.getUserId());
//        messageMap.put("roomId", chatMessage.getRoomId());
//        messageMap.put("content", chatMessage.getContent());
//        messageMap.put("type", chatMessage.getType().name()); // Enum은 String으로 변환
//        //messageMap.put("timestamp", String.valueOf(chatMessage.getTimestamp())); // long은 String으로 변환
//
//        // MapRecord를 생성하여 Stream에 추가
//        MapRecord<String, String, String> record = StreamRecords.mapBacked(messageMap)
//            .withStreamKey(coffeeChatStreamKey);
//        RecordId recordId = streamOperations.add(record);
//        System.out.println(
//            "[ChatController.handleCoffeeChatMessage] - recordId " + recordId + "를 가지는 "
//                + coffeeChatStreamKey + "메시지가 스트림에 등록되었습니다.");
//
//        // 브로커를 통해 해당 채팅방을 구독한 모든 클라이언트에게 메시지 브로드캐스팅
//        // Destination: /topic/chatrooms/{roomId}
//        messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, chatMessage);


    }
}