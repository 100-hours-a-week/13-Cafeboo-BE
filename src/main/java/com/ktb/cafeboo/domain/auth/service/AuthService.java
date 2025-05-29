package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.TokenRefreshResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.TokenBlacklistReason;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public TokenRefreshResponse refreshAccessToken(String refreshToken, String accessToken) {
        log.info("[AuthService.refreshAccessToken] accessToken 갱신 시작");

        String userId = jwtProvider.validateRefreshToken(refreshToken);
        log.info("[AuthService.refreshAccessToken] refreshToken 유효성 검사 완료");

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> {
                    log.warn("[AuthService.refreshAccessToken] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        if (!refreshToken.equals(user.getRefreshToken())) {
            log.warn("[AuthService.refreshAccessToken] 저장된 refreshToken과 일치하지 않음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_MISMATCH);
        }

        tokenBlacklistService.addToBlacklist(accessToken, userId, TokenBlacklistReason.REFRESH);

        String newAccessToken = jwtProvider.createAccessToken(
                user.getId().toString(),
                user.getLoginType().name(),
                user.getRole().name()
        );

        log.info("[AuthService.refreshAccessToken] accessToken 재발급 완료");

        return new TokenRefreshResponse(userId, newAccessToken);
    }

    @Transactional
    public void logout(String accessToken, Long userId) {
        log.info("[AuthService.logout] 로그아웃 처리 시작");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[AuthService.logout] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        tokenBlacklistService.addToBlacklist(accessToken, userId.toString(), TokenBlacklistReason.LOGOUT);

        user.updateRefreshToken(null);
        userRepository.save(user);

        log.info("[AuthService.logout] refreshToken 초기화 및 로그아웃 처리 완료");
    }
}
