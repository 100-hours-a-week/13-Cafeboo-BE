package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.MonthlyCaffeineReportResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/reports/monthly")
public class MonthlyReportController {
    private final WeeklyReportService weeklyReportService;

    @GetMapping
    ResponseEntity<ApiResponse<MonthlyCaffeineReportResponse>> getMonthlyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = true, name = "year") String year,
        @RequestParam(required = true, name = "month") String month){
        log.info("[MonthlyReportController.getMonthlyCaffeineReport] 월간 카페인 리포트 요청 수신 - year={}, month={}", year, month);

        Long userId = userDetails.getId();

        try{
            MonthlyCaffeineReportResponse response = weeklyReportService.getWeeklyStatisticsForMonth(userId, year, month);
            return ResponseEntity.ok(ApiResponse.of(SuccessStatus.MONTHLY_CAFFEINE_REPORT_SUCCESS, response));
        }
        catch(CustomApiException e){
            return ResponseEntity
                .status(ErrorStatus.INVALID_PARAMETER.getStatus()) // ErrorStatus에서 정의된 HTTP 상태 코드 사용
                .body(ApiResponse.of(ErrorStatus.INVALID_PARAMETER, null));
        }
    }
}
