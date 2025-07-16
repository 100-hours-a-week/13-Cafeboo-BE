package com.ktb.cafeboo.global.infra.kafka.service;

import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, StompMessagePublish> kafkaTemplate;

    // ✨ sendMessage 메서드 파라미터 타입 변경 ✨
    public void sendMessage(String topic, StompMessagePublish message) {
        log.info("[KafkaProducerService] Kafka 메시지 전송 중 - Topic: {}, Message: {}", topic, message);
        kafkaTemplate.send(topic, message);
        log.info("[KafkaProducerService] Kafka 메시지 전송 완료!");
    }
}