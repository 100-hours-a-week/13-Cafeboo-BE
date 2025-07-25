package com.ktb.cafeboo.global.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisCacheMetrics redisCacheMetrics;
    private final ObjectMapper objectMapper;
    private static final int MAX_CACHE_SIZE = 100;

    public record CachedMessagesResult(List<MessageDto> messages, boolean hasNext, boolean needsWarmup) {}

    public void warmUpCache(Long roomId, List<MessageDto> messageDtoList) {
        String key = "chat:room:" + roomId + ":messages";

        // 최신 메시지가 앞에 오도록 순서 뒤집기
        List<String> jsonList = messageDtoList.stream()
                .map(this::convertToJson)
                .filter(Objects::nonNull)
                .toList();

        redisTemplate.delete(key); // 기존 캐시 제거
        if (!jsonList.isEmpty()) {
            redisTemplate.opsForList().leftPushAll(key, jsonList);
            redisTemplate.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);
            redisTemplate.expire(key, Duration.ofHours(24));
        }

        log.info("[warmUpCache] 캐시 워밍업 완료 - roomId={}, count={}", roomId, jsonList.size());
    }

    public void cacheMessage(Long roomId, MessageDto messageDto) {
        String key = "chat:room:" + roomId + ":messages";

        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.FALSE.equals(hasKey)) {
            log.debug("[cacheMessage] 캐시 키 없음 - 저장 생략, roomId={}", roomId);
            return;
        }

        String messageJson = convertToJson(messageDto);
        redisTemplate.opsForList().leftPush(key, messageJson);
        redisTemplate.opsForList().trim(key, 0, MAX_CACHE_SIZE - 1);

        log.debug("[cacheMessage] 캐시 저장 - roomId={}, messageId={}", roomId, messageDto.messageId());
    }

    public CachedMessagesResult getMessagesIfCacheHit(Long roomId, String cursor, int limit) {
        String key = "chat:room:" + roomId + ":messages";
        List<String> cachedJsonList = redisTemplate.opsForList().range(key, 0, MAX_CACHE_SIZE - 1);

        if (cachedJsonList == null || cachedJsonList.isEmpty()) {
            redisCacheMetrics.incrementMiss();
            log.debug("[getMessagesIfCacheHit] 캐시 미스 - roomId={}, 캐시 비어있음", roomId);
            return new CachedMessagesResult(List.of(), false, true); // 캐시 miss
        }

        List<MessageDto> cachedMessages = new ArrayList<>(
                cachedJsonList.stream()
                        .map(json -> {
                            try {
                                return objectMapper.readValue(json, MessageDto.class);
                            } catch (JsonProcessingException e) {
                                log.warn("[getMessagesIfCacheHit] 메시지 역직렬화 실패 - json={}", json);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList()
        );
        Collections.reverse(cachedMessages);

        if (cursor.equals("0")) {
            List<MessageDto> result = cachedMessages.subList(0, Math.min(limit, cachedMessages.size()));
            boolean hasNext = cachedMessages.size() > limit;
            redisCacheMetrics.incrementHit();
            log.debug("[getMessagesIfCacheHit] 캐시 히트 - roomId={}, cursor=0, resultSize={}, hasNext={}",
                    roomId, result.size(), hasNext);
            return new CachedMessagesResult(result, hasNext, false);
        }

        int index = IntStream.range(0, cachedMessages.size())
                .filter(i -> cachedMessages.get(i).messageId().equals(cursor))
                .findFirst()
                .orElse(-1);

        if (index == -1) {
            // 캐시 대상이 아닌 범위의 요청이므로 캐시 미스 통계에 포함하지 않음
            //redisCacheMetrics.incrementMiss();
            log.debug("[ChatMessageCacheService] 캐시된 100개 범위를 벗어난 커서 요청: {}", cursor);
            return new CachedMessagesResult(List.of(), false, false); // 커서 못 찾음 ⇒ miss
        }

        int toIndex = Math.min(index + limit, cachedMessages.size());
        List<MessageDto> result = cachedMessages.subList(index, toIndex);

        boolean hasNext = toIndex < cachedMessages.size();

        redisCacheMetrics.incrementHit();
        log.debug("[getMessagesIfCacheHit] 캐시 히트 - roomId={}, cursor={}, resultSize={}, hasNext={}",
                roomId, cursor, result.size(), hasNext);
        return new CachedMessagesResult(result, hasNext, false);
    }

    private String convertToJson(MessageDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }
}
