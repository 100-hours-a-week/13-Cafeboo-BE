package com.ktb.cafeboo.global.infra.kafka.producer;

import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageProducer {
    public static final String CHAT_MESSAGES_TOPIC = "coffeechat-messages";

    private final KafkaTemplate<String, StompMessagePublish> kafkaTemplate;

    public void publishChatMessage(StompMessagePublish message) {
        String messageKey = String.valueOf(message.getCoffeechatId());

        log.info("Sending message to Kafka topic '{}' with key '{}': {}",
            CHAT_MESSAGES_TOPIC, messageKey, message);

        kafkaTemplate.send(CHAT_MESSAGES_TOPIC, messageKey, message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Message sent successfully to partition {} with offset {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send message: {}", ex.getMessage(), ex);
                }
            });
    }
}