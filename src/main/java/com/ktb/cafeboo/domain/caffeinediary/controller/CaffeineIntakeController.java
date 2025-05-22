package com.ktb.cafeboo.domain.caffeinediary.controller;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.DailyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caffeine-intakes")
@RequiredArgsConstructor
@Slf4j
public class CaffeineIntakeController {
    private final CaffeineIntakeService caffeineIntakeService;

    @PostMapping
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> recordCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CaffeineIntakeRequest request) {
        log.info("[POST /api/v1/caffeine-intakes] 카페인 섭취 기록 요청 수신");

        Long userId = userDetails.getId();

        // 1. 서비스 메서드 호출
        CaffeineIntakeResponse response = caffeineIntakeService.recordCaffeineIntake(userId, request);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_RECORDED, response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> updateCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id,
        @RequestBody CaffeineIntakeRequest request) {
        log.info("[PATCH /api/v1/caffeine-intakes/{}] 카페인 섭취 수정 요청 수신", id);

        // 1. 서비스 메서드 호출
        CaffeineIntakeResponse response = caffeineIntakeService.updateCaffeineIntake(id, request);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_UPDATED, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>>deleteCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id) {
        log.info("[DELETE /api/v1/caffeine-intakes/{}] 카페인 섭취 삭제 요청 수신", id);

        // 1. 서비스 메서드 호출
        caffeineIntakeService.deleteCaffeineIntake(id);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_DELETED, null));
    }

    @GetMapping
    @RequestMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyCaffeineDiaryResponse>>getCaffeineIntakeDiary(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("year") String targetYear,
        @RequestParam("month") String targetMonth){
        log.info("[GET /api/v1/caffeine-intakes/monthly] 월간 카페인 다이어리 조회 요청 - year={}, month={}", targetYear, targetMonth);

        Long userId = userDetails.getId();

        MonthlyCaffeineDiaryResponse response = caffeineIntakeService.getCaffeineIntakeDiary(userId, targetYear, targetMonth);

        return ResponseEntity.ok(ApiResponse.of(
            SuccessStatus.MONTHLY_CAFFEINE_CALENDAR_SUCCESS, response));
    }

    @GetMapping
    @RequestMapping("/daily")
    public ResponseEntity<ApiResponse<DailyCaffeineDiaryResponse>> getDailyCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("date") String date) {
        log.info("[GET /api/v1/caffeine-intakes/daily] 일간 카페인 다이어리 조회 요청 - date={}", date);

        Long userId = userDetails.getId();
        DailyCaffeineDiaryResponse response = caffeineIntakeService.getDailyCaffeineIntake(userId, date);

        return ResponseEntity.ok(ApiResponse.of(
            SuccessStatus.DAILY_CAFFEINE_CALENDAR_SUCCESS, response));
    }
}
