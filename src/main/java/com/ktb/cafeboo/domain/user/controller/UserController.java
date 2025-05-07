package com.ktb.cafeboo.domain.user.controller;

import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.service.UserHealthInfoService;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.*;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import com.ktb.cafeboo.global.util.AuthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserHealthInfoService userHealthInfoService;

    @GetMapping("/email")
    public ResponseEntity<ApiResponse<EmailDuplicationResponse>> checkEmailDuplication(
            @RequestParam String email) {
        EmailDuplicationResponse response = userService.isEmailDuplicated(email);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.EMAIL_DUPLICATION_CHECK_SUCCESS, response));
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
}
