package com.ktb.cafeboo.domain.auth.controller;

import com.ktb.cafeboo.domain.auth.dto.KakaoLoginRequest;
import com.ktb.cafeboo.domain.auth.dto.LoginResponse;
import com.ktb.cafeboo.domain.auth.dto.TokenRefreshResponse;
import com.ktb.cafeboo.domain.auth.service.AuthService;
import com.ktb.cafeboo.domain.auth.service.KakaoOauthService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
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
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @RequestBody KakaoLoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = kakaoOauthService.login(request.getCode());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(14 * 24 * 60 * 60) // 14일
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.LOGIN_SUCCESS, loginResponse.withoutRefreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshAccessToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomApiException(ErrorStatus.REFRESH_TOKEN_INVALID);
        }

        TokenRefreshResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.TOKEN_REFRESH_SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response) {

        Long userId = userDetails.getUserId();
        authService.logout(userId);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0) // 즉시 만료
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());

        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
