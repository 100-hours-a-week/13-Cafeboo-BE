package com.ktb.cafeboo.domain.caffeinediary.controller;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caffeine-intakes")
@RequiredArgsConstructor
public class CaffeineIntakeController {
    private final CaffeineIntakeService caffeineIntakeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> recordCaffeineIntake(
        /*@AuthenticationPrincipal UserDetails userDetails,  더미 유저 사용으로 주석 처리*/
        @RequestBody CaffeineIntakeRequest request) {

        // 1. 더미 유저 조회
        User dummyUser = userService.getUserById(2L); // 예시: ID가 1인 유저를 조회
        // Long userId = Long.parseLong(userDetails.getUsername()); // Assuming username contains userId -> 더미 유저 사용
        Long userId = dummyUser.getId();

        // 2. 서비스 메서드 호출
        CaffeineIntakeResponse response = caffeineIntakeService.recordCaffeineIntake(dummyUser, request);

        // 3. 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<CaffeineIntakeResponse>builder()
                .status(201)
                .code("CAFFEINE_INTAKE_RECORDED")
                .message("카페인 섭취 내역이 성공적으로 등록되었습니다.")
                .data(response)
                .build());
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> updateCaffeineIntake(
        /*@AuthenticationPrincipal UserDetails userDetails,  더미 유저 사용으로 주석 처리*/
        @RequestBody CaffeineIntakeRequest request) {

        // 1. 서비스 메서드 호출
        CaffeineIntakeResponse response = caffeineIntakeService.updateCaffeineIntake(1L, request);

        // 2. 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<CaffeineIntakeResponse>builder()
                .status(200)
                .code("CAFFEINE_INTAKE_UPDATED")
                .message("카페인 섭취 내역이 성공적으로 수정되었습니다.")
                .data(response)
                .build());
    }
}
