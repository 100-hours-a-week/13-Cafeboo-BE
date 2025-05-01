package com.ktb.cafeboo.domain.auth.controller;

import com.ktb.cafeboo.domain.auth.dto.KakaoLoginRequest;
import com.ktb.cafeboo.domain.auth.dto.KakaoLoginResponse;
import com.ktb.cafeboo.domain.auth.service.KakaoOauthService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoOauthService kakaoOauthService;

    @PostMapping("/oauth")
    public ResponseEntity<Void> redirectToOauth(@RequestParam("type") String type) {
        String redirectUrl;

        if ("kakao".equalsIgnoreCase(type)) {
            redirectUrl = kakaoOauthService.buildKakaoAuthorizationUrl();
        } else {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 타입입니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return ResponseEntity.status(HttpStatus.SEE_OTHER).headers(headers).build();
    }

    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        log.info("1. 컨트롤러 정상 진입");
        KakaoLoginResponse loginResponse = kakaoOauthService.login(request.getCode());
        return ResponseEntity.ok(ApiResponse.success(200,"LOGIN_SUCCESS", "로그인 성공", loginResponse));
    }
}
