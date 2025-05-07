package com.ktb.cafeboo.domain.auth.controller;

import com.ktb.cafeboo.domain.auth.dto.KakaoLoginRequest;
import com.ktb.cafeboo.domain.auth.dto.KakaoLoginResponse;
import com.ktb.cafeboo.domain.auth.service.KakaoOauthService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
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
    public ResponseEntity<ApiResponse<String>> redirectToOauth(@RequestParam("type") String type) {

        if (!"kakao".equalsIgnoreCase(type)) {
            throw new CustomApiException(ErrorStatus.UNSUPPORTED_SOCIAL_LOGIN_TYPE);
        }

        String redirectUrl = kakaoOauthService.buildKakaoAuthorizationUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return ResponseEntity.status(HttpStatus.SEE_OTHER).headers(headers).build();
    }

    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        KakaoLoginResponse loginResponse = kakaoOauthService.login(request.getCode());
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.LOGIN_SUCCESS, loginResponse));
    }
}
