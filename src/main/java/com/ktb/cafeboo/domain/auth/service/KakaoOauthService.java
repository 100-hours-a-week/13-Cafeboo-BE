package com.ktb.cafeboo.domain.auth.service;

import com.ktb.cafeboo.domain.auth.dto.KakaoLoginResponse;
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

    public KakaoLoginResponse login(String code) {
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

        return KakaoLoginResponse.builder()
                .userId(user.getId().toString())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .requiresOnboarding(requiresOnboarding)
                .build();
    }
}
