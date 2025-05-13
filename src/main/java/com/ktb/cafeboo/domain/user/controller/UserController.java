package com.ktb.cafeboo.domain.user.controller;

import com.ktb.cafeboo.domain.auth.service.KakaoOauthService;
import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.service.UserAlarmSettingService;
import com.ktb.cafeboo.domain.user.service.UserCaffeineInfoService;
import com.ktb.cafeboo.domain.user.service.UserHealthInfoService;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.*;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import com.ktb.cafeboo.global.util.AuthChecker;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoOauthService kakaoOauthService;
    private final UserHealthInfoService userHealthInfoService;
    private final UserCaffeineInfoService userCaffeineInfoService;
    private final UserAlarmSettingService userAlarmSettingService;

    @GetMapping("/email")
    public ResponseEntity<ApiResponse<EmailDuplicationResponse>> checkEmailDuplication(
            @RequestParam String email) {
        EmailDuplicationResponse response = userService.isEmailDuplicated(email);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.EMAIL_DUPLICATION_CHECK_SUCCESS, response));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserProfileResponse response = userService.getUserProfile(userId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.BASIC_PROFILE_FETCH_SUCCESS, response));
    }


    @PostMapping("/{userId}/health")
    public ResponseEntity<ApiResponse<UserHealthInfoCreateResponse>> createHealthInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserHealthInfoCreateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserHealthInfoCreateResponse response = userHealthInfoService.create(userId, request);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.HEALTH_PROFILE_CREATION_SUCCESS, response));
    }

    @PatchMapping("/{userId}/health")
    public ResponseEntity<ApiResponse<UserHealthInfoUpdateResponse>> updateHealthInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserHealthInfoUpdateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserHealthInfoUpdateResponse response = userHealthInfoService.update(userId, request);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.HEALTH_PROFILE_UPDATE_SUCCESS, response));
    }

    @GetMapping("/{userId}/health")
    public ResponseEntity<ApiResponse<UserHealthInfoResponse>> getHealthInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserHealthInfoResponse response = userHealthInfoService.getHealthInfo(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.HEALTH_PROFILE_FETCH_SUCCESS, response));
    }

    @PostMapping("/{userId}/caffeine")
    public ResponseEntity<ApiResponse<UserCaffeineInfoCreateResponse>> createCaffeineInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserCaffeineInfoCreateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserCaffeineInfoCreateResponse response = userCaffeineInfoService.create(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.CAFFEINE_PROFILE_CREATION_SUCCESS, response));
    }

    @PatchMapping("/{userId}/caffeine")
    public ResponseEntity<ApiResponse<UserCaffeineInfoUpdateResponse>> updateCaffeineInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserCaffeineInfoUpdateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserCaffeineInfoUpdateResponse response = userCaffeineInfoService.update(userId, request);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_PROFILE_UPDATE_SUCCESS, response));
    }

    @GetMapping("/{userId}/caffeine")
    public ResponseEntity<ApiResponse<UserCaffeineInfoResponse>> getCaffeineInfo(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserCaffeineInfoResponse response = userCaffeineInfoService.getCaffeineInfo(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_PROFILE_FETCH_SUCCESS, response));
    }

    @PostMapping("/{userId}/alarm")
    public ResponseEntity<ApiResponse<UserAlarmSettingCreateResponse>> createAlarmSetting(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserAlarmSettingCreateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserAlarmSettingCreateResponse response = userAlarmSettingService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.ALARM_SETTING_CREATION_SUCCESS, response));
    }

    @PatchMapping("/{userId}/alarm")
    public ResponseEntity<ApiResponse<UserAlarmSettingUpdateResponse>> updateAlarmSetting(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserAlarmSettingUpdateRequest request
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserAlarmSettingUpdateResponse response = userAlarmSettingService.update(userId, request);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.ALARM_SETTING_UPDATE_SUCCESS, response));
    }

    @GetMapping("/{userId}/alarm")
    public ResponseEntity<ApiResponse<UserAlarmSettingResponse>> getAlarmSetting(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        UserAlarmSettingResponse response = userAlarmSettingService.get(userId);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.ALARM_SETTING_FETCH_SUCCESS, response));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ) {
        AuthChecker.checkOwnership(userId, userDetails.getUserId());

        kakaoOauthService.disconnectKakaoAccount(userId);
        userService.deleteUser(userId);

        // refreshToken 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());

        return ResponseEntity.noContent().build();  // 204 No Content
    }

}
