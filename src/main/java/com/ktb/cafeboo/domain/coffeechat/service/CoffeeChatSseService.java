package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.sse.CoffeeChatUpdatePayload;
import com.ktb.cafeboo.domain.coffeechat.dto.sse.DeletedCoffeeChatPayload;
import com.ktb.cafeboo.domain.coffeechat.dto.sse.NewCoffeeChatPayload;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CoffeeChatSseService {

    // 클라이언트별 SseEmitter 저장소 (userId 기준)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SSE 연결 구독
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10분 타임아웃

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            emitter.complete();
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            emitter.complete();
            emitters.remove(userId);
        });


        // 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connection established.")); // 503 에러 방지를 위한 더미 데이터
        } catch (IOException e) {
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

}
