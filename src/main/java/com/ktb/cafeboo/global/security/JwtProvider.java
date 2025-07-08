package com.ktb.cafeboo.global.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final long accessTokenValidity = 24 * 60 * 60 * 1000;  // 1일
    private final long refreshTokenValidity = 14 * 24 * 60 * 60 * 1000;  // 14일

    public String createAccessToken(String userId, String loginType, String role) {
        return createToken(userId, loginType, role, accessTokenValidity);
    }

    public String createRefreshToken(String userId, String loginType, String role) {
        return createToken(userId, loginType, role, refreshTokenValidity);
    }

    private String createToken(String userId, String loginType, String role, long validityInMillis) {
        Date now = new Date();
        return JWT.create()
                .withSubject(userId)
                .withClaim("loginType", loginType)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + validityInMillis))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public String validateAccessToken(String token) {
        try {
            String subject = JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token)
                    .getSubject();
            if (subject == null) {
                throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_INVALID);
            }
            return subject;
        } catch (TokenExpiredException e) {
            throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_EXPIRED);
        } catch (JWTVerificationException e) {
            throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_INVALID);
        }
    }

    public String validateRefreshToken(String token) {
        try {
            String subject = JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token)
                    .getSubject();
            if (subject == null) {
                throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_INVALID);
            }
            return subject;
        } catch (TokenExpiredException e) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_EXPIRED);
        } catch (JWTVerificationException e) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_INVALID);
        }
    }

    public long getRemainingExpiration(String token) {
        Date expiresAt = JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token)
                .getExpiresAt();

        return expiresAt.getTime() - System.currentTimeMillis();
    }

    public DecodedJWT decodeExpiredToken(String token) {
        try {
            return JWT.decode(token); // 검증 없이 디코드만
        } catch (JWTDecodeException e) {
            throw new CustomApiException(ErrorStatus.ACCESS_TOKEN_INVALID);
        }
    }
}