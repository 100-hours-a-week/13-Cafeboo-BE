package com.ktb.cafeboo.global.infra.kafka.consumer;

import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.global.infra.kafka.producer.KafkaMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageListener implements AcknowledgingMessageListener<String, StompMessagePublish> {
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaMessageProducer.CHAT_MESSAGES_TOPIC,
                   containerFactory = "kafkaListenerContainerFactory")
    @Override
    public void onMessage(ConsumerRecord<String, StompMessagePublish> record, Acknowledgment acknowledgment) {
        String roomId = record.key();
        StompMessagePublish stompMessage = record.value();

        log.info("[KafkaChatMessageListener] Received message from Kafka for WebSockets. Topic: '{}', Partition: {}, Offset: {}. Room ID: {}, Message ID: {}",
            record.topic(), record.partition(), record.offset(), roomId, stompMessage.getMessageId());

        try{
            messagingTemplate.convertAndSend("/topic/chatrooms/" + roomId, stompMessage);
            acknowledgment.acknowledge();
        }
        catch (Exception e) {

        }
    }
}
