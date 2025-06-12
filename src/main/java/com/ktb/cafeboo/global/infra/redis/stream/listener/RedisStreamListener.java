package com.ktb.cafeboo.global.infra.redis.stream.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import com.ktb.cafeboo.global.enums.MessageType;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// ⭐ MapRecord<String, String, String>으로 변경 ⭐
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
// ⭐ StreamListener<String, MapRecord<String, String, String>> 로 변경 ⭐
public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    // ⭐ MapRecord<String, String, String> 으로 변경 ⭐
    public void onMessage(MapRecord<String, String, String> message) {
        log.error("[RedisStreamListener] === onMessage method called! === Record ID: {}",
            message.getId());

        String streamKey = message.getStream();
        RecordId recordId = message.getId();

        StompMessagePublish stompMessage = null;
        try {
            Map<String, String> rawData = message.getValue(); // Base64 인코딩된 String 값들을 가진 Map

            Map<String, Object> decodedData = new HashMap<>();
            for (Map.Entry<String, String> entry : rawData.entrySet()) {
                String key = entry.getKey();
                String encodedValue = entry.getValue();

                // _class 필드는 계속 무시
                if ("_class".equals(key)) {
                    continue;
                }

                if (encodedValue != null && !encodedValue.isEmpty()) {
                    String cleanEncodedValue = encodedValue;
                    if (encodedValue.startsWith("\"") && encodedValue.endsWith("\"")) {
                        cleanEncodedValue = encodedValue.substring(1, encodedValue.length() - 1);
                    }
                    try {
                        byte[] decodedBytes = Base64.getDecoder().decode(cleanEncodedValue);
                        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

                        // ⭐⭐⭐ 각 필드에 맞는 타입으로 변환 및 디코딩 ⭐⭐⭐
                        if ("coffeechatId".equals(key) || "sender.userId".equals(key)) { // userId는 sender 안에 있으므로 "sender.userId"
                            try {
                                decodedData.put(key, Long.parseLong(decodedString));
                            } catch (NumberFormatException e) {
                                log.warn("Failed to parse Long for key {}: {} - keeping as String.", key, decodedString);
                                decodedData.put(key, decodedString); // 파싱 실패 시 String으로 유지
                            }
                        } else if ("messageType".equals(key)) { // ⭐ messageType 처리 추가 ⭐
                            try {
                                // Enum.valueOf()는 대소문자를 구분하므로, Enum 정의에 따라 toUpperCase() 등 적용 필요
                                decodedData.put(key, MessageType.valueOf(decodedString)); // Enum으로 바로 변환
                            } catch (IllegalArgumentException e) {
                                log.warn("Failed to parse MessageType for key {}: {} - keeping as String. Error: {}", key, decodedString, e.getMessage());
                                decodedData.put(key, decodedString); // 변환 실패 시 String으로 유지 (추후 ObjectMapper가 처리 못 할 수 있음)
                            }
                        }
                        else if ("sentAt".equals(key)) {
                            // LocalDateTime 필드는 String으로 유지하고 ObjectMapper가 처리하도록 합니다.
                            decodedData.put(key, decodedString);
                        }
                        else if ("sender.name".equals(key) || "sender.profileImageUrl".equals(key) || "content".equals(key) || "messageId".equals(key)) {
                            // 나머지 String 필드
                            decodedData.put(key, decodedString);
                        } else {
                            // 예상치 못한 키는 디코딩된 문자열로 저장 (일반적인 경우)
                            decodedData.put(key, decodedString);
                        }

                    } catch (IllegalArgumentException e) { // Base64 디코딩 실패
                        log.warn("Base64 decoding failed for key {}: {} - using original value. Error: {}", key, encodedValue, e.getMessage());
                        decodedData.put(key, cleanEncodedValue);
                    }
                } else {
                    decodedData.put(key, encodedValue); // null 또는 빈 문자열은 그대로 유지
                }
            }

            // ⭐⭐⭐ SenderInfo 내부 객체를 Map으로 다시 구성 (중요!) ⭐⭐⭐
            // "sender.userId", "sender.name", "sender.profileImageUrl" 필드는
            // ObjectMapper가 StompMessagePublish.sender 필드에 매핑할 때 SenderInfo 객체로 다시 묶여야 합니다.
            // ObjectMapper의 convertValue는 점(.)으로 구분된 필드를 자동으로 중첩 객체로 인식하지 못할 수 있습니다.
            // 따라서 수동으로 SenderInfo 맵을 생성하여 'sender' 키에 넣어줘야 합니다.
            Map<String, Object> senderMap = new HashMap<>();
            if (decodedData.containsKey("sender.userId")) {
                senderMap.put("userId", decodedData.remove("sender.userId"));
            }
            if (decodedData.containsKey("sender.name")) {
                senderMap.put("name", decodedData.remove("sender.name"));
            }
            if (decodedData.containsKey("sender.profileImageUrl")) {
                senderMap.put("profileImageUrl", decodedData.remove("sender.profileImageUrl"));
            }
            if (!senderMap.isEmpty()) {
                decodedData.put("sender", senderMap); // 'sender' 키에 SenderInfo Map 추가
            }

            stompMessage = objectMapper.convertValue(decodedData, StompMessagePublish.class);

            log.info("[RedisStreamListener.onMessage] - 수신된 StompMessagePublish: {}", stompMessage);
            if (stompMessage != null) {
                log.info("  messageId: {}", stompMessage.getMessageId());
                log.info("  coffeechatId: {}", stompMessage.getCoffeechatId());
                log.info("  sender.userId: {}",
                    stompMessage.getSender() != null ? stompMessage.getSender().getUserId()
                        : "null");
                log.info("  content: {}", stompMessage.getContent());
            } else {
                log.error("[RedisStreamListener.onMessage] - stompMessage가 null입니다. 수동 역직렬화 실패.");
                return;
            }

            String roomId = String.valueOf(stompMessage.getCoffeechatId());
            log.info("[RedisStreamListener.onMessage] - Stream '{}'에서 메시지 '{}'를 수신했습니다.", streamKey,
                recordId.getValue());

            messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, stompMessage);

            log.info(
                "[RedisStreamListener.onMessage] - WebSocket /topic/chatrooms/{} 로 메시지를 전송했습니다.",
                roomId);
        } catch (Exception e) {
            log.error("[RedisStreamListener.onMessage] - 메시지 처리 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}