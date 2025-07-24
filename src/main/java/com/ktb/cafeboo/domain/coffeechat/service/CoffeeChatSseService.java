package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.sse.CoffeeChatUpdatePayload;
import com.ktb.cafeboo.domain.coffeechat.dto.sse.DeletedCoffeeChatPayload;
import com.ktb.cafeboo.domain.coffeechat.dto.sse.NewCoffeeChatPayload;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoffeeChatSseService {

    // 클라이언트별 SseEmitter 저장소 (userId 기준)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 구독
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10분 타임아웃

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> cleanupEmitter(userId, emitter, "onCompletion"));
        emitter.onTimeout(() -> cleanupEmitter(userId, emitter, "onTimeout"));
        emitter.onError(e -> cleanupEmitter(userId, emitter, "onError: " + e.getMessage()));


        // 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connection established."));
            log.info("[SSE] 연결 성공 - userId: {}", userId);
        } catch (IOException e) {
            log.warn("[SSE] 연결 실패 - userId: {}, error: {}", userId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendCurrentMemberCountUpdate(Long coffeeChatId, Integer currentMemberCount) {
        CoffeeChatUpdatePayload payload = new CoffeeChatUpdatePayload(
                coffeeChatId.toString(),
                currentMemberCount
        );

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("current-member-count")
                        .data(payload));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    public void sendNewCoffeeChat(CoffeeChat chat) {
        if (emitters.isEmpty()) {
            log.warn("[SSE] 등록된 Emitter 없음 - 이벤트: new-coffeechat");
            return;
        }

        LocalDateTime meetingTime = chat.getMeetingTime();
        NewCoffeeChatPayload payload = new NewCoffeeChatPayload(
                chat.getId().toString(),
                chat.getName(),
                meetingTime.toLocalDate().toString(),
                meetingTime.toLocalTime().toString(),
                chat.getMaxMemberCount(),
                chat.getCurrentMemberCount(),
                chat.getTagNames(),
                chat.getAddress(),
                new NewCoffeeChatPayload.Writer(
                        chat.getWriter().getId(),
                        chat.getWriter().getNickname(),
                        chat.getWriter().getProfileImageUrl(),
                        true
                )
        );

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-coffeechat")
                        .data(payload));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    public void sendDeletedCoffeeChat(Long coffeeChatId) {
        DeletedCoffeeChatPayload payload = new DeletedCoffeeChatPayload(coffeeChatId.toString());

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("deleted-coffeechat")
                        .data(payload));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    // 1분마다 heartbeat 보내는 스케줄러 메서드
    @Scheduled(fixedRate = 60000)
    public void sendHeartbeatToAll() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    private void cleanupEmitter(Long userId, SseEmitter emitter, String reason) {
        log.warn("[SSE] Emitter 제거 - userId: {}, 이유: {}", userId, reason);
        emitter.complete();
        emitters.remove(userId);
    }
}
