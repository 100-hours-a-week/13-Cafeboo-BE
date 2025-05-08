package com.ktb.cafeboo.global.infra.kakao;

import com.ktb.cafeboo.domain.auth.dto.KakaoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KakaoTokenClient {

    private final WebClient webClient = WebClient.builder().baseUrl("https://kauth.kakao.com").build();

    public KakaoTokenResponse getToken(String code, String clientId, String redirectUri, String grantType) {
        return webClient.post()
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
