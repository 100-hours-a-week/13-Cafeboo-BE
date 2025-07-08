package com.ktb.cafeboo.global.config;

import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import software.amazon.awssdk.utils.ImmutableMap;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerConfiguration {

    @Bean
    public ProducerFactory<String, StompMessagePublish> producerFactory() {
       Map<String, Object> producerConfigurations =
           ImmutableMap.<String, Object>builder()
               .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
               .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
               .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class)
               .put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false)
               .build();

        return new DefaultKafkaProducerFactory<>(producerConfigurations);
    }

    // KafkaTemplate을 생성하는 Bean 메서드
    @Bean
    public KafkaTemplate<String, StompMessagePublish> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory(){
        Map<String, Object> producerConfigurations =
            ImmutableMap.<String, Object>builder()
                .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
                .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class)
                .put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false) // DLQ 메시지도 JSON으로 직렬화 (오류 정보 포함 가능)
                .build();
        return new DefaultKafkaProducerFactory<>(producerConfigurations);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

}
