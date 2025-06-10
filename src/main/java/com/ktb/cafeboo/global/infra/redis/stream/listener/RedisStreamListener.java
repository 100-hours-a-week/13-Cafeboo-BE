package com.ktb.cafeboo.global.infra.redis.stream.listener;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamListener implements StreamListener<String, ObjectRecord<String, CoffeeChatMessage>> {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(ObjectRecord<String, CoffeeChatMessage> message) {
        String streamKey = message.getStream();
        RecordId recordId = message.getId();
        CoffeeChatMessage coffeechatMessage = message.getValue();
        String roomId = String.valueOf(coffeechatMessage.getChat().getId());

        log.info("[RedisStreamListener.onMessage] - Stream {}에 메시지 {}를 수신받았습니다.", streamKey, coffeechatMessage);

        try{
            //WebSocket을 통해 채팅방의 모든 구독자에게 메시지 전송
            //
            messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, message);
            log.info("[RedisStreamListener.onMessage] - Stream {}에서 message {}를 WebSocket topic /topic/{} 로 전송했습니다..", streamKey, recordId.getValue(), roomId);
        }
        catch (Exception e) {
            log.error("[RedisStreamListener.onMessage] - Stream {}에서 메시지 {}의 처리에 실패했습니다.", streamKey, recordId.getValue());
        }
    }
}
