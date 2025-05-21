package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.LoginResponse;
import com.ktb.cafeboo.domain.auth.model.OauthToken;
import com.ktb.cafeboo.domain.auth.repository.OauthTokenRepository;
import com.ktb.cafeboo.domain.user.dto.UserAlarmSettingCreateRequest;
import com.ktb.cafeboo.domain.user.service.UserAlarmSettingService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoTokenResponse;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoUserResponse;
import com.ktb.cafeboo.global.infra.kakao.client.KakaoTokenClient;
import com.ktb.cafeboo.global.infra.kakao.client.KakaoUserClient;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOauthService {
    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserClient kakaoUserClient;
    private final UserRepository userRepository;
    private final OauthTokenRepository oauthTokenRepository;
    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final UserAlarmSettingService userAlarmSettingService;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.kakao.scope}")
    private String scopes;


    @Value("${spring.security.oauth2.client.registration.kakao.authorization-grant-type}")
    private String grantType;


    public String buildKakaoAuthorizationUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scopes)
                .build()
                .toUriString();
    }

    @Transactional
    public LoginResponse login(String code, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String resolvedRedirectUri;

        if (origin != null && origin.contains("localhost")) {
            resolvedRedirectUri = "http://localhost:5173/oauth/kakao/callback";
        } else {
            resolvedRedirectUri = this.redirectUri;
        }

        KakaoTokenResponse kakaoToken = kakaoTokenClient.getToken(code, clientId, resolvedRedirectUri, grantType);
        KakaoUserResponse kakaoUser = kakaoUserClient.getUserInfo(kakaoToken.getAccessToken());

        Optional<User> userOpt = userRepository.findByOauthIdAndLoginType(kakaoUser.getId(), LoginType.KAKAO);
        User user;
        boolean requiresOnboarding;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            requiresOnboarding = !userService.hasCompletedOnboarding(user);
        } else {
            user = userRepository.save(User.fromKakao(kakaoUser));

            // 기본 알람 설정
            UserAlarmSettingCreateRequest userAlarmSetting = new UserAlarmSettingCreateRequest(
                false,
                false,
                false
            );
            userAlarmSettingService.create(user.getId(), userAlarmSetting);

            requiresOnboarding = true;
        }

        String accessToken = jwtProvider.createAccessToken(
                String.valueOf(user.getId()),
                LoginType.KAKAO.name(),
                user.getRole().name()
        );
        String refreshToken = jwtProvider.createRefreshToken(
                String.valueOf(user.getId()),
                LoginType.KAKAO.name(),
                user.getRole().name()
        );
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        // 카카오 Oauth 토큰 저장
        oauthTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        existingToken -> {
                            existingToken.update(
                                    kakaoToken.getAccessToken(),
                                    kakaoToken.getRefreshToken(),
                                    kakaoToken.getExpiresIn()
                            );
                        },
                        () -> {
                            OauthToken newToken = OauthToken.of(
                                    user,
                                    kakaoToken.getAccessToken(),
                                    kakaoToken.getRefreshToken(),
                                    LoginType.KAKAO,
                                    kakaoToken.getExpiresIn()
                            );
                            oauthTokenRepository.save(newToken);
                        }
                );

        return new LoginResponse(
                user.getId().toString(),
                accessToken,
                requiresOnboarding,
                refreshToken
        );
    }

    public void logoutFromKakao(Long userId) {
        tryWithTokenRefresh(userId, kakaoUserClient::logout);
    }

    public void disconnectKakaoAccount(Long userId) {
        tryWithTokenRefresh(userId, kakaoUserClient::unlinkUser);
    }

    public String refreshAccessTokenIfExpired(Long userId) {
        OauthToken oauthToken = oauthTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.OAUTH_TOKEN_NOT_FOUND));

        try {
            KakaoTokenResponse response = kakaoTokenClient.refreshAccessToken(
                    oauthToken.getRefreshToken(),
                    clientId
            );

            oauthToken.update(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getExpiresIn()
            );
            oauthTokenRepository.save(oauthToken);

            return response.getAccessToken();
        } catch (CustomApiException e) {
            log.warn("[카카오 토큰 갱신 실패 - 커스텀 예외] userId: {}, status: {}", userId, e.getErrorCode().getStatus());
            throw e;
        } catch (Exception e) {
            log.error("[카카오 토큰 갱신 실패 - 시스템 예외] userId: {}, message: {}", userId, e.getMessage());
            throw new CustomApiException(ErrorStatus.KAKAO_TOKEN_REFRESH_FAILED);
        }
    }

    private void tryWithTokenRefresh(Long userId, java.util.function.Consumer<String> action) {
        OauthToken oauthToken = oauthTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.OAUTH_TOKEN_NOT_FOUND));

        try {
            action.accept(oauthToken.getAccessToken());
        } catch (Exception e) {
            log.warn("[OauthTokenRefresh] accessToken이 만료되어 재발급 후 재시도합니다. userId: {}", userId);
            String newAccessToken = refreshAccessTokenIfExpired(userId);
            action.accept(newAccessToken);
        }
    }
}
