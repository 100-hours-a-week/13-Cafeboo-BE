package com.ktb.cafeboo.global.config;

import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import software.amazon.awssdk.utils.ImmutableMap;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfiguration {

    @Value("${spring.application.name}") // 기본값도 지정할 수 있습니다.
    private String kafkaConsumerGroupIdPrefix;

    @Value("${kafka.dlq.topic}")
    private String dlqTopic;

    @Value("${spring.data.kafka.host}:${spring.data.kafka.port}")
    private String kafkaBootstrapServers;

    @Bean
    public ConsumerFactory<String, StompMessagePublish> consumerFactory(){
        String kafkaConsumerGroupId = kafkaConsumerGroupIdPrefix + java.util.UUID.randomUUID().toString();

        JsonDeserializer<StompMessagePublish> jsonDeserializer = new JsonDeserializer<>(StompMessagePublish.class, false);
        jsonDeserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<StompMessagePublish> errorHandlingValueDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);

        Map<String, Object> consumerConfigurations =
                ImmutableMap.<String, Object>builder()
                        .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
                        .put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId)
                        .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                        .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class)
                        .put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, jsonDeserializer.getClass().getName())
                        .put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName())
                        .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
                        .put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                        .build();

        log.info("[KafkaConsumerConfiguration.consumerFactory] group id {}를 가지는 consumerFactory 생성", kafkaConsumerGroupId);

        return new DefaultKafkaConsumerFactory<>(consumerConfigurations, new StringDeserializer(), errorHandlingValueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StompMessagePublish> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, StompMessagePublish> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(Runtime.getRuntime().availableProcessors());
        return factory;
    }
}
