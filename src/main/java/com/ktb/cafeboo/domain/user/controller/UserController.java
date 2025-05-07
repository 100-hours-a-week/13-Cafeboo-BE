package com.ktb.cafeboo.domain.user.controller;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateResponse;
import com.ktb.cafeboo.domain.user.service.UserHealthInfoService;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.*;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
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
        if (!userId.equals(userDetails.getUserId())) {
            throw new CustomApiException(ErrorStatus.ACCESS_DENIED);
        }

        UserHealthInfoCreateResponse response = userHealthInfoService.create(userId, request);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.HEALTH_PROFILE_CREATION_SUCCESS, response));
    }
}
