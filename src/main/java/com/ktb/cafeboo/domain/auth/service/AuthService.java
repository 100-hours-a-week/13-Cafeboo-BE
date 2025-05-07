package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.TokenRefreshResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        String userId;

        try {
            userId = jwtProvider.validateToken(refreshToken);
            if (userId == null) {
                throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_INVALID);
            }
        } catch (RuntimeException e) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.createAccessToken(
                user.getEmail(),
                user.getLoginType().name(),
                user.getRole().name()
        );

        return new TokenRefreshResponse(newAccessToken);
    }
}