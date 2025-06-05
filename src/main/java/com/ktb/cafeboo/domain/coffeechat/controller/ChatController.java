package com.ktb.cafeboo.domain.coffeechat.controller;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.Message;
import com.ktb.cafeboo.domain.user.model.User;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate; // 메시지 브로커로 메시지를 라우팅하는 역할
    private final RedisTemplate<String, Object> redisTemplate; // RedisTemplate 주입
    private StreamOperations<String, Object, Object> streamOperations; // StreamOperations 선언

    @PostConstruct
    private void init() {
        this.streamOperations = redisTemplate.opsForStream();
    }

    // 클라이언트에서 /app/chatrooms/{roomId}로 메시지를 보내면 이 메서드가 처리
    @MessageMapping("/chatrooms/{roomId}")
    public void handleCoffeeChatMessage(@DestinationVariable String roomId, @Payload Message chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // 클라이언트에서 보낸 메시지 로그
        User sender = chatMessage.getSender();
        CoffeeChat coffeeChat = chatMessage.getChat();

        log.info("[ChatController.handleCoffeeChatMessage] - 유저 {}로부터 커피챗 {}에 보내는 메시지를 받았습니다: {}",
            sender.getId(), roomId,
            chatMessage.getContent());

        String coffeeChatStreamKey = "coffeechat:" + roomId + ":stream";

        // ChatMessage 객체를 Map<String, String>으로 변환
        Map<String, String> messageMap = new HashMap<>();
        messageMap.put("userId", sender.getId().toString());
        messageMap.put("roomId", coffeeChat.getId().toString());
        messageMap.put("content", chatMessage.getContent());
        messageMap.put("type", chatMessage.getType().name()); // Enum은 String으로 변환
        //messageMap.put("timestamp", String.valueOf(chatMessage.getTimestamp())); // long은 String으로 변환

        // MapRecord를 생성하여 Stream에 추가
        MapRecord<String, String, String> record = StreamRecords.mapBacked(messageMap)
            .withStreamKey(coffeeChatStreamKey);
        RecordId recordId = streamOperations.add(record);
        System.out.println(
            "[ChatController.handleCoffeeChatMessage] - recordId " + recordId + "를 가지는 "
                + coffeeChatStreamKey + "메시지가 스트림에 등록되었습니다.");

        // 브로커를 통해 해당 채팅방을 구독한 모든 클라이언트에게 메시지 브로드캐스팅
        // Destination: /topic/chatrooms/{roomId}
        messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, chatMessage);
    }

    // WebSocket/STOMP 연결 시 호출 (옵션)
    // @EventListener
    // public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    //     SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
    //     System.out.println("WebSocket Connected: " + headers.getSessionId() + " by " + headers.getUser().getName());
    //     // 사용자 로그인 정보 등을 활용하여 초기 데이터 전송 가능
    // }

    // WebSocket/STOMP 연결 해제 시 호출 (옵션)
    // @EventListener
    // public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    //     SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
    //     System.out.println("WebSocket Disconnected: " + headers.getSessionId());
    // }
}