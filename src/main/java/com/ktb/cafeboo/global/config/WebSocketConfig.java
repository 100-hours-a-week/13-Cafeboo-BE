package com.ktb.cafeboo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP를 이용한 메시지 브로커 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. /topic으로 시작하는 메시지를 브로커가 처리하도록 설정 (Pub/Sub)
        // 2. /queue로 시작하는 메시지를 브로커가 처리하도록 설정 (개인 메시지)
        // Spring의 Simple Broker (인메모리) 사용. 실제 서비스에서는 Redis, Kafka, RabbitMQ 등의 외부 브로커 사용 권장.
        config.enableSimpleBroker("/topic", "/queue").setHeartbeatValue(new long[] {10000, 10000});

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 접두사 (Controller로 라우팅됨)
        config.setApplicationDestinationPrefixes("/app");

        // 특정 사용자에게 메시지를 라우팅할 때 사용할 접두사 (STOMP `/user` destination)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결을 위한 엔드포인트 설정
        // 클라이언트가 http://localhost:8080/ws 로 연결 시도
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*") // 모든 Origin 허용 (CORS). 실제 운영에서는 특정 Origin만 허용해야 함
            .withSockJS()
            .setDisconnectDelay(300 * 1000);
    }
}