package com.ktb.cafeboo.global.infra.kakao.client;

import com.ktb.cafeboo.global.infra.kakao.dto.KakaoTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoTokenClient {

    private final WebClient kakaoWebClient;

    public KakaoTokenResponse getToken(String code, String clientId, String redirectUri, String grantType) {
        return kakaoWebClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=" + grantType
                        + "&client_id=" + clientId
                        + "&redirect_uri=" + redirectUri
                        + "&code=" + code)
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }
}
