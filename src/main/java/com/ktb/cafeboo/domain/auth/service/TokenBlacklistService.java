package com.ktb.cafeboo.domain.auth.service;

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

    public void addToBlacklist(String accessToken) {
        long remainingMillis = jwtProvider.getRemainingExpiration(accessToken);

        redisTemplate.opsForValue().set(
                PREFIX + accessToken,
                "logout",
                remainingMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + accessToken));
    }
}