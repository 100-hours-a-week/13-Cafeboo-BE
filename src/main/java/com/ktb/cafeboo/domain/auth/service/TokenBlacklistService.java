package com.ktb.cafeboo.domain.auth.service;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ktb.cafeboo.global.enums.TokenBlacklistReason;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    private static final String PREFIX = "blacklist:";

    public void addToBlacklist(String accessToken, String userId, TokenBlacklistReason reason) {
        long remainingMillis;

        try {
            // 정상적인 유효 토큰이면 이 경로로
            remainingMillis = jwtProvider.getRemainingExpiration(accessToken);
        } catch (TokenExpiredException e) {
            // 만료된 토큰도 블랙리스트 등록: decode만 수행
            DecodedJWT decodedJWT = jwtProvider.decodeExpiredToken(accessToken);
            Date expiresAt = decodedJWT.getExpiresAt();
            remainingMillis = expiresAt.getTime() - System.currentTimeMillis();

            if (remainingMillis <= 0) {
                // 만료 시간이 이미 지났다면 저장하지 않음
                log.warn("만료된 토큰이므로 블랙리스트에 저장하지 않음.");
                return;
            }
        }

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