package com.ktb.cafeboo.domain.caffeinediary.controller;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.DailyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

        try{
            // 1. 서비스 메서드 호출
            log.info("[POST /api/v1/caffeine-intakes] 카페인 섭취 기록 실행");
            CaffeineIntakeResponse response = caffeineIntakeService.recordCaffeineIntake(userId, request);
            log.info("[POST /api/v1/caffeine-intakes] 카페인 섭취 기록 완료");
            // 2. 응답 반환
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_RECORDED, response));
        }
        catch(CustomApiException e){
            log.error("[POST /api/v1/caffeine-intakes] 카페인 섭취 기록 오류 : {}", e.getMessage());
            return ResponseEntity
                .status(e.getErrorCode().getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(e.getErrorCode(), null));
        }
        catch (Exception e) {
            log.error("[POST /api/v1/caffeine-intakes] 카페인 섭취 기록 오류 : {}", e.getMessage());
            return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> updateCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id,
        @RequestBody CaffeineIntakeRequest request) {
        log.info("[PATCH /api/v1/caffeine-intakes/{}] 카페인 섭취 수정 요청 수신", id);

        try{
            // 1. 서비스 메서드 호출
            log.info("[PATCH /api/v1/caffeine-intakes/{}] 카페인 섭취 수정 시작", id);
            CaffeineIntakeResponse response = caffeineIntakeService.updateCaffeineIntake(id, request);
            log.info("[PATCH /api/v1/caffeine-intakes/{}] 카페인 섭취 수정 완료", id);
            // 2. 응답 반환
            return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_UPDATED, response));
        }
        catch (CustomApiException e){
            return ResponseEntity
                .status(e.getErrorCode().getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(e.getErrorCode(), null));
        }
        catch (Exception e) {
            return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>>deleteCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id) {
        log.info("[DELETE /api/v1/caffeine-intakes/{}] 카페인 섭취 삭제 요청 수신", id);

        try{
            // 1. 서비스 메서드 호출
            caffeineIntakeService.deleteCaffeineIntake(id);

            // 2. 응답 반환
            return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
        }
        catch (CustomApiException e){
            return ResponseEntity
                .status(ErrorStatus.INTAKE_NOT_FOUND.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTAKE_NOT_FOUND, null));
        }
        catch (Exception e) {
            return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }

    /**
     * 카페인 다이어리 달력 조회
     * @param userDetails
     * @param targetYear
     * @param targetMonth
     * @return
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyCaffeineDiaryResponse>>getCaffeineIntakeDiary(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("year") String targetYear,
        @RequestParam("month") String targetMonth){
        log.info("[GET /api/v1/caffeine-intakes/monthly] 월간 카페인 다이어리 조회 요청 - year={}, month={}", targetYear, targetMonth);

        Long userId = userDetails.getId();

        try{
            MonthlyCaffeineDiaryResponse response = caffeineIntakeService.getCaffeineIntakeDiary(userId, targetYear, targetMonth);

            return ResponseEntity.ok(ApiResponse.of(
                SuccessStatus.MONTHLY_CAFFEINE_CALENDAR_SUCCESS, response));
        }
        catch (CustomApiException e){
            return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.BAD_REQUEST, null));
        }
        catch (Exception e) {
            return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }


    /**
     * 카페인 다이어리 일별 조회
     * @param userDetails
     * @param date
     * @return
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyCaffeineDiaryResponse>> getDailyCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("date") String date) {
        log.info("[GET /api/v1/caffeine-intakes/daily] 일간 카페인 다이어리 조회 요청 - date={}", date);

        Long userId = userDetails.getId();

        try{
            DailyCaffeineDiaryResponse response = caffeineIntakeService.getDailyCaffeineIntake(userId, date);

            return ResponseEntity.ok(ApiResponse.of(
                SuccessStatus.DAILY_CAFFEINE_CALENDAR_SUCCESS, response));
        }
        catch (CustomApiException e){
            return ResponseEntity
                .status(ErrorStatus.BAD_REQUEST.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.BAD_REQUEST, null));
        }
        catch (Exception e) {
            return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INTERNAL_SERVER_ERROR, null));
        }
    }
}
