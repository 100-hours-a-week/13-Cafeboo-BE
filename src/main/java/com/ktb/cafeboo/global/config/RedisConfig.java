package com.ktb.cafeboo.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.cafeboo.domain.coffeechat.dto.StompMessagePublish;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.UUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Data
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    private String oddListKey;
    private String evenListKey;
    private String oddEvenStream;
    private String consumerGroupName; // 이 값은 이제 ChatService에서 userId 기반으로 동적으로 생성되므로, 더 이상 사용되지 않을 수 있습니다.
    private String recordCacheKey;
    private long streamPollTimeout;
    private String consumerName; // 개별 컨슈머 이름. @PostConstruct에서 동적으로 생성
    private String failureListKey;

    @PostConstruct // ApplicationConfig의 @PostConstruct 로직도 여기로 옮깁니다.
    public void setConsumerName() throws UnknownHostException {
        // 각 서버 인스턴스에 고유한 컨슈머 이름을 부여
        consumerName = InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * redis의 연결 정보 설정
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(host);
        redisConfiguration.setPort(port);
        if (!password.isBlank()) {
            redisConfiguration.setPassword(RedisPassword.of(password));
        }
        return new LettuceConnectionFactory(redisConfiguration);
    }

    /**
     * redis의 pub/sub 메시지를 처리하는 listener 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * Spring에서 Redis에 Key - Value 형식의 데이터를 읽고 쓰는데 사용하는 구조.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // Key 직렬화 (Stream Key 및 Map Key)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화 (Stream Value 및 Map Value)
        // ✨ 이 부분이 가장 중요합니다. StringRedisSerializer를 사용하면 JSON 문자열이 그대로 저장됩니다. ✨
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.setThreadNamePrefix("redis-stream-listener-");
        taskExecutor.initialize();

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainerOptions.builder()
                .batchSize(1)
                .executor(taskExecutor)
                .pollTimeout(Duration.ofSeconds(1))
                // MapRecord를 직접 받을 것이므로 serializer와 targetType은 설정하지 않습니다.
                // Spring Data Redis가 MapRecord의 세 번째 인수를 기반으로 내부적으로 String으로 디코딩하여 제공할 것입니다.
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
            StreamMessageListenerContainer.create(redisConnectionFactory(), options);

        container.start();
        log.info("[RedisConfig] - StreamMessageListenerContainer started.");
        return container;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 처리 모듈 등록
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
