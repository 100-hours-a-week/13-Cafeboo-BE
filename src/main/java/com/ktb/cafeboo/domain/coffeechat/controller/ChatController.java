package com.ktb.cafeboo.domain.coffeechat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ktb.cafeboo.domain.coffeechat.dto.StompMessage;
import com.ktb.cafeboo.domain.coffeechat.service.ChatService;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.kafka.service.KafkaProducerService;
import jakarta.annotation.PostConstruct;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
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
    public void handleCoffeeChatMessage(@DestinationVariable String roomId, @Payload StompMessage chatMessage, SimpMessageHeaderAccessor headerAccessor, Principal principal)
        throws Exception {

        log.info("[ChatController.handleCoffeeChatMessage] - handleCoffeeChatMessage 호출");
        // 클라이언트에서 보낸 메시지 로그

        log.info("[ChatController.handleCoffeeChatMessage] - 유저 {}로부터 커피챗 {}에 보내는 메시지를 받았습니다: {}",
            chatMessage.getSenderId(), chatMessage.getCoffeechatId(),
            chatMessage.getMessage());

        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            log.warn("Received message for room {} without a valid session ID. Message content: {}", roomId, chatMessage.getMessage());
            return; // 유효한 세션 ID가 없으면 STOMP 연결이 활성화되지 않을 것이기에 메시지 처리 중단
        }

        try{
            chatService.handleNewMessage(roomId, chatMessage);
        }
        catch (CustomApiException e){
            log.error("[ChatController.handleCoffeeChatMessage] - 메시지 전송 실패 - 검열");
            throw e;
        }
        catch (JsonProcessingException e){
            log.error("[ChatController.handleCoffeeChatMessage] - 메시지 직렬화 실패 오류. roomId: {}, {}", roomId, e.getMessage());
            throw e;
        }
        catch (Exception e) {
            log.error("[ChatController.handleCoffeeChatMessage] - 메시지 전송 실패");
            throw e;
        }
    }

    @MessageExceptionHandler(CustomApiException.class) // CustomApiException을 잡습니다.
    @SendToUser("/queue/errors") // 이 메시지를 클라이언트의 /user/queue/errors로 보냅니다.
    public String handleCustomApiException(CustomApiException ex) {
        log.error("[ChatController.MessageExceptionHandler] - CustomApiException 발생: {}", ex.getMessage());
        // 클라이언트에게 보낼 메시지 (응답 본문)
        // ex.getMessage()는 "부적절한 표현을 담은 메시지"가 될 것입니다.
        return "메시지 전송 실패: " + ex.getMessage();
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("[ChatController.MessageExceptionHandler] - IllegalArgumentException 발생: {}", ex.getMessage(), ex);
        return "처리할 수 없는 요청: " + ex.getMessage();
    }

    @MessageExceptionHandler(Exception.class) // 가장 포괄적인 예외 처리
    @SendToUser("/queue/errors")
    public String handleGenericException(Exception ex) {
        log.error("[ChatController.MessageExceptionHandler] - 알 수 없는 오류 발생: {}", ex.getMessage(), ex);
        // 사용자에게는 일반적인 오류 메시지를, 자세한 내용은 서버 로그에 기록
        return "서버 오류: 알 수 없는 문제가 발생했습니다. 다시 시도해 주세요.";
    }
}