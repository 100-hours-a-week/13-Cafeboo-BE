package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.global.enums.TokenBlacklistReason;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    private static final String PREFIX = "blacklist:";

    public void addToBlacklist(String accessToken, String userId, TokenBlacklistReason reason) {
        long remainingMillis = jwtProvider.getRemainingExpiration(accessToken);
        String value = reason.formatWithUserId(userId);

        redisTemplate.opsForValue().set(
                PREFIX + accessToken,
                value,
                remainingMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + accessToken));
    }
}