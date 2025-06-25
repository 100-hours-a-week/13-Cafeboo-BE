package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.global.enums.MessageType;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisStreamSubscriber {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void startListening() {
        new Thread(() -> {
            StreamOffset<String> offset = StreamOffset.fromStart("coffeechat:test:stream");

            while (true) {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    StreamReadOptions.empty().block(Duration.ofMillis(2000)).count(10),
                    offset
                );

                if (records != null) {
                    for (MapRecord<String, Object, Object> record : records) {
                        Map<Object, Object> data = record.getValue();

                        CoffeeChatMessage message = CoffeeChatMessage.builder()// Enum이라면 String에서 Enum으로 변환 필요
                            // 예: CoffeeChatMessageType.valueOf(rawData.get("messageType"))
                            .sender((CoffeeChatMember) data.get("sender"))
                            .coffeeChat((CoffeeChat) data.get("chat"))
                            .content((String) data.get("content"))
                            .type(MessageType.valueOf((String) data.get("type")))
                            .build();

                        messagingTemplate.convertAndSend("/topic/chatrooms/" + message.getCoffeeChat().getId(), message);
                    }
                }
            }
        }).start();
    }
}
