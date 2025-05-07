package com.ktb.cafeboo.domain.user.controller;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/email")
    public ResponseEntity<ApiResponse<EmailDuplicationResponse>> checkEmailDuplication(
            @RequestParam String email) {
        EmailDuplicationResponse response = userService.isEmailDuplicated(email);
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.EMAIL_DUPLICATION_CHECK_SUCCESS, response));
    }
}
