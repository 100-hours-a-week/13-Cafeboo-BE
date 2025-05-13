package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.LoginResponse;
import com.ktb.cafeboo.domain.auth.model.OauthToken;
import com.ktb.cafeboo.domain.auth.repository.OauthTokenRepository;
import com.ktb.cafeboo.domain.user.dto.UserAlarmSettingCreateRequest;
import com.ktb.cafeboo.domain.user.service.UserAlarmSettingService;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoTokenResponse;
import com.ktb.cafeboo.global.infra.kakao.dto.KakaoUserResponse;
import com.ktb.cafeboo.global.infra.kakao.client.KakaoTokenClient;
import com.ktb.cafeboo.global.infra.kakao.client.KakaoUserClient;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    public LoginResponse login(String code) {
        KakaoTokenResponse kakaoToken = kakaoTokenClient.getToken(code, clientId, redirectUri, grantType);
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
            UserAlarmSettingCreateRequest userAlarmSetting = UserAlarmSettingCreateRequest.builder()
                    .alarmBeforeSleep(false)
                    .alarmWhenExceedIntake(false)
                    .alarmForChat(false)
                    .build();
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

        return LoginResponse.builder()
                .userId(user.getId().toString())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .requiresOnboarding(requiresOnboarding)
                .build();
    }

    public String buildKakaoLogoutUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://kauth.kakao.com/oauth/logout")
                .queryParam("client_id", clientId)
                .queryParam("logout_redirect_uri", redirectUri)
                .build()
                .toUriString();
    }

    public void disconnectKakaoAccount(Long userId) {
        oauthTokenRepository.findByUserId(userId)
                .ifPresent(oauthToken -> {
                    kakaoUserClient.unlinkUser(oauthToken.getAccessToken());
                });
    }
}
