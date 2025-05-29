package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.TokenRefreshResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.TokenBlacklistReason;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public TokenRefreshResponse refreshAccessToken(String refreshToken, String accessToken) {
        String userId = jwtProvider.validateRefreshToken(refreshToken);

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_MISMATCH);
        }

        tokenBlacklistService.addToBlacklist(accessToken, userId, TokenBlacklistReason.REFRESH);

        String newAccessToken = jwtProvider.createAccessToken(
                user.getId().toString(),
                user.getLoginType().name(),
                user.getRole().name()
        );

        return TokenRefreshResponse.builder()
                .userId(userId)
                .accessToken(newAccessToken)
                .build();
    }

    @Transactional
    public void logout(String accessToken, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        tokenBlacklistService.addToBlacklist(accessToken, userId.toString(), TokenBlacklistReason.LOGOUT);

        user.updateRefreshToken(null);
        userRepository.save(user);
    }
}