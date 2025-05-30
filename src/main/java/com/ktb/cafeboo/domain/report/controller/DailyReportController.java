package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.service.DailyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/daily")
public class DailyReportController {
    private final DailyReportService dailyReportService;

    /**
     * 사용자의 일일 카페인 섭취 현황 데이터를 조회합니다.
     * 상세 스펙은 @see <a href="https://freckle-pipe-840.notion.site/1ddb43be904c8078b6ecfbeaf23369f6">일일 섭취 현황 조회 API 스펙 문서</a>
//     * @param user 현재 인증된 사용자
     * @param targetDate 조회할 날짜 (yyyy-MM-dd), 미입력시 오늘 날짜
     * @return 일일 카페인 섭취 리포트
     * @throws IllegalArgumentException 잘못된 요청 파라미터
//     * @throws AuthenticationException 인증 실패
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DailyCaffeineReportResponse>> getDailyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false, name = "date") String targetDate) {
        log.info("[DailyReportController.getDailyCaffeineReport] 일일 카페인 리포트 요청 수신 - date={}", targetDate);

        Long userId = userDetails.getId();
        LocalDate date;

        try {
            date = (targetDate == null || targetDate.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(targetDate);
        } catch (Exception e) {
            log.error("[DailyReportController.getDailyCaffeineReport] 잘못된 날짜 형식입니다. - targetDate={}", targetDate);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        // 일일 리포트 생성
        DailyCaffeineReportResponse response = dailyReportService.createDailyReport(userId, date, LocalTime.now());

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.DAILY_CAFFEINE_REPORT_SUCCESS, response));
    }
}
