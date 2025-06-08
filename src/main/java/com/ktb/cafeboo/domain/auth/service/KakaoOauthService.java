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
import com.ktb.cafeboo.global.infra.s3.S3Uploader;
import com.ktb.cafeboo.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

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
    private final S3Uploader s3Uploader;

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
        log.info("[KakaoOauthService.login] 카카오 로그인 요청 수신");
        String origin = request.getHeader("Origin");
        String resolvedRedirectUri;

        if (origin != null && origin.contains("localhost")) {
            resolvedRedirectUri = "http://localhost:5173/oauth/kakao/callback";
        } else {
            resolvedRedirectUri = this.redirectUri;
        }

        KakaoTokenResponse kakaoToken = kakaoTokenClient.getToken(code, clientId, resolvedRedirectUri, grantType);
        KakaoUserResponse kakaoUser = kakaoUserClient.getUserInfo(kakaoToken.getAccessToken());

        User user = getOrCreateUser(kakaoUser);
        boolean requiresOnboarding = !userService.hasCompletedOnboarding(user);

        if (requiresOnboarding) {
            log.info("[KakaoOauthService.login] 신규 사용자 가입 - userId={}", user.getId());
        } else {
            log.info("[KakaoOauthService.login] 기존 사용자 로그인 - userId={}", user.getId());
        }

        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);

        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        saveOrUpdateOauthToken(user, kakaoToken);

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
        log.info("[KakaoOauthService.refreshAccessTokenIfExpired] accessToken 갱신 시도 - userId={}", userId);

        OauthToken oauthToken = oauthTokenRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[KakaoOauthService.refreshAccessTokenIfExpired] OauthToken 없음 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.OAUTH_TOKEN_NOT_FOUND);
                });

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

            log.info("[KakaoOauthService.refreshAccessTokenIfExpired] accessToken 갱신 성공 - userId={}", userId);
            return response.getAccessToken();

        } catch (CustomApiException e) {
            log.warn("[KakaoOauthService.refreshAccessTokenIfExpired] 카카오 토큰 갱신 실패 (CustomException) - userId={}, status={}", userId, e.getErrorCode().getStatus());
            throw e;

        } catch (Exception e) {
            log.error("[KakaoOauthService.refreshAccessTokenIfExpired] 카카오 토큰 갱신 실패 (Exception) - userId={}, message={}", userId, e.getMessage());
            throw new CustomApiException(ErrorStatus.KAKAO_TOKEN_REFRESH_FAILED);
        }
    }

    private KakaoTokenResponse getKakaoToken(String code) {
        return kakaoTokenClient.getToken(code, clientId, redirectUri, grantType);
    }

    private KakaoUserResponse getKakaoUserInfo(String accessToken) {
        return kakaoUserClient.getUserInfo(accessToken);
    }

    private User getOrCreateUser(KakaoUserResponse kakaoUser) {
        return userRepository.findByOauthIdAndLoginType(kakaoUser.getId(), LoginType.KAKAO)
                .map(user -> {
                    // 기존 유저인데 profileImageUrl이 비어있다면
                    if (user.getProfileImageUrl() == null) {
                        String kakaoImageUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();
                        log.info("kakaoImageUrl={}", kakaoImageUrl);

                        String profileImageUrl;

                        if (kakaoImageUrl != null && !kakaoImageUrl.isBlank()) {
                            try (InputStream is = new URL(kakaoImageUrl).openStream()) {
                                long contentLength = is.available();
                                String contentType = URLConnection.guessContentTypeFromStream(is);
                                profileImageUrl = s3Uploader.uploadProfileImage(is, contentLength, contentType);
                            } catch (IOException e) {
                                profileImageUrl = s3Uploader.getDefaultProfileImageUrl();
                            }
                        } else {
                            profileImageUrl = s3Uploader.getDefaultProfileImageUrl();
                        }
                        user.updateProfileImage(profileImageUrl);
                        userRepository.save(user);
                    }

                    return user;
                })
                .orElseGet(() -> {
                    String kakaoImageUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();
                    String profileImageUrl;

                    if (kakaoImageUrl != null && !kakaoImageUrl.isBlank()) {
                        try (InputStream is = new URL(kakaoImageUrl).openStream()) {
                            long contentLength = is.available();
                            String contentType = URLConnection.guessContentTypeFromStream(is);
                            profileImageUrl = s3Uploader.uploadProfileImage(is, contentLength, contentType);
                        } catch (IOException e) {
                            profileImageUrl = s3Uploader.getDefaultProfileImageUrl();
                        }
                    } else {
                        profileImageUrl = s3Uploader.getDefaultProfileImageUrl();
                    }
                    log.info("kakaoImageUrl={}", kakaoImageUrl);
                    log.info("profileImageUrl={}", profileImageUrl);
                    User newUser = User.fromKakao(kakaoUser, profileImageUrl);

                    User savedUser = userRepository.save(newUser);
                    userAlarmSettingService.create(
                            savedUser.getId(),
                            new UserAlarmSettingCreateRequest(false, false, false)
                    );
                    return newUser;
                });
    }

    private String generateAccessToken(User user) {
        return jwtProvider.createAccessToken(
                user.getId().toString(),
                user.getLoginType().name(),
                user.getRole().name()
        );
    }

    private String generateRefreshToken(User user) {
        return jwtProvider.createRefreshToken(
                user.getId().toString(),
                user.getLoginType().name(),
                user.getRole().name()
        );
    }

    private void saveOrUpdateOauthToken(User user, KakaoTokenResponse kakaoToken) {
        oauthTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        existingToken -> existingToken.update(
                                kakaoToken.getAccessToken(),
                                kakaoToken.getRefreshToken(),
                                kakaoToken.getExpiresIn()
                        ),
                        () -> oauthTokenRepository.save(OauthToken.of(
                                user,
                                kakaoToken.getAccessToken(),
                                kakaoToken.getRefreshToken(),
                                LoginType.KAKAO,
                                kakaoToken.getExpiresIn()
                        ))
                );
    }

    private void tryWithTokenRefresh(Long userId, Consumer<String> action) {
        OauthToken oauthToken = oauthTokenRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[KakaoOauthService.tryWithTokenRefresh] OauthToken 없음 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.OAUTH_TOKEN_NOT_FOUND);
                });

        try {
            action.accept(oauthToken.getAccessToken());
        } catch (Exception e) {
            log.warn("[KakaoOauthService.tryWithTokenRefresh] accessToken 만료로 재발급 후 재시도 - userId={}", userId);
            String newAccessToken = refreshAccessTokenIfExpired(userId);
            action.accept(newAccessToken);
        }
    }
}
