package com.ktb.cafeboo.global.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final long accessTokenValidity = 60 * 60 * 1000;  // 60분
    private final long refreshTokenValidity = 14 * 24 * 60 * 60 * 1000;  // 14일

    public String createAccessToken(String userId, String loginType, String role) {
        return createToken(userId, loginType, role, accessTokenValidity);
    }

    public String createRefreshToken(String userId, String loginType, String role) {
        return createToken(userId, loginType, role, refreshTokenValidity);
    }

    private String createToken(String userId, String loginType, String role, long validityInMillis) {
        return JWT.create()
                .withSubject(userId)
                .withClaim("loginType", loginType)
                .withClaim("role", role)
                .withExpiresAt(new Date(System.currentTimeMillis() + validityInMillis))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public String validateToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                    .build()
                    .verify(token)
                    .getSubject();  // userId 반환
        } catch (TokenExpiredException e) {
            throw new RuntimeException("Token expired");
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
