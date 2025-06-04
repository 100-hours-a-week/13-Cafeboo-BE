package com.ktb.cafeboo.global.config;

import com.ktb.cafeboo.domain.coffeechat.model.Message;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

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
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StreamMessageListenerContainer<String, ?> stringStreamMessageListenerContainer(RedisConnectionFactory connectionFactory){
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5); // 기본 스레드 수
        taskExecutor.setMaxPoolSize(10); // 최대 스레드 수
        taskExecutor.setQueueCapacity(200);
        taskExecutor.setThreadNamePrefix("redis-stream-listener-");
        taskExecutor.initialize();

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ?> options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .batchSize(10) // 한 번에 처리할 메시지 수
                .executor(taskExecutor) // 사용할 스레드 풀
                .pollTimeout(Duration.ofSeconds(1)) // 메시지가 없을 때 폴링 타임아웃
                .targetType(Message.class) // 메시지 역직렬화 대상 타입
                .build();

        StreamMessageListenerContainer<String, ?>container = StreamMessageListenerContainer.create(connectionFactory, options);
        container.start();

        return container;
    }
}
