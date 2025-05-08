package com.ktb.cafeboo.global.infra.kakao.client;

import com.ktb.cafeboo.global.infra.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
@RequiredArgsConstructor
public class KakaoUserClient {

    private final WebClient kakaoApiWebClient;

    public KakaoUserResponse getUserInfo(String accessToken) {
        return kakaoApiWebClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}
