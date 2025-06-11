package com.ktb.cafeboo.domain.auth.controller;

import com.ktb.cafeboo.domain.auth.dto.KakaoLoginRequest;
import com.ktb.cafeboo.domain.auth.dto.LoginResponse;
import com.ktb.cafeboo.domain.auth.dto.TokenRefreshResponse;
import com.ktb.cafeboo.domain.auth.service.AuthService;
import com.ktb.cafeboo.domain.auth.service.KakaoOauthService;
import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoOauthService kakaoOauthService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<String>> redirectToOauth(@RequestParam("type") String type) {
        log.info("[POST /api/v1/auth/oauth] 소셜 로그인 리다이렉트 요청 - type={}", type);

        if (!"kakao".equalsIgnoreCase(type)) {
            log.warn("[POST /api/v1/auth/oauth] 지원하지 않는 소셜 로그인 타입 - type={}", type);
            throw new CustomApiException(ErrorStatus.UNSUPPORTED_SOCIAL_LOGIN_TYPE);
        }

        String redirectUrl = kakaoOauthService.buildKakaoAuthorizationUrl();
        log.info("[POST /api/v1/auth/oauth] 카카오 인증 URL 생성 완료");

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirectUrl));
        return ResponseEntity.status(HttpStatus.SEE_OTHER).headers(headers).build();
    }

    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @RequestBody KakaoLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        log.info("[POST /api/v1/auth/kakao] 카카오 로그인 요청 수신");

  
        LoginResponse loginResponse = kakaoOauthService.login(request.code(), httpRequest);
        log.info("[POST /api/v1/auth/kakao] 카카오 로그인 성공");

        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResponse.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(14 * 24 * 60 * 60) // 14일
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        log.info("[POST /api/v1/auth/kakao] refreshToken 쿠키 설정 완료");

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.LOGIN_SUCCESS, loginResponse.withoutRefreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshAccessToken(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        log.info("[POST /api/v1/auth/refresh] accessToken 갱신 요청 수신");

        String accessToken = authHeader.replace("Bearer ", "");

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("[POST /api/v1/auth/refresh] refreshToken 누락 또는 빈 값");
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_INVALID);
        }

        TokenRefreshResponse response = authService.refreshAccessToken(refreshToken, accessToken);
        log.info("[POST /api/v1/auth/refresh] accessToken 재발급 완료");

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.TOKEN_REFRESH_SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response) {      
        log.info("[POST /api/v1/auth/logout] 로그아웃 요청 수신");

        String accessToken = authHeader.replace("Bearer ", "");

        Long userId = userDetails.getUserId();

        kakaoOauthService.logoutFromKakao(userId);
        log.info("[POST /api/v1/auth/logout] 카카오 로그아웃 완료");
      
        authService.logout(accessToken, userId);
        log.info("[POST /api/v1/auth/logout] 내부 세션 정리 및 토큰 삭제 완료");

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0) // 즉시 만료
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());
        log.info("[POST /api/v1/auth/logout] refreshToken 쿠키 제거 완료");

        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
