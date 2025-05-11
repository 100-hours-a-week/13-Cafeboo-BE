package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.MonthlyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.YearlyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.service.MonthlyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/yearly")
public class YearlyReportController {
    private final MonthlyReportService monthlyReportService;

    @GetMapping
    ResponseEntity<ApiResponse<YearlyCaffeineReportResponse>> getYearlyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false, name = "year") String targetYear) {

        Long userId = userDetails.getId();
        Year year = Year.of(Integer.parseInt(targetYear));


        YearlyCaffeineReportResponse response = monthlyReportService.getMonthlyReportForYear(userId, year);
        //List<MonthlyReport> monthlyReports = monthlyReportService.getMonthlyReportForYear(userId, year);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.YEARLY_CAFFEINE_REPORT_SUCCESS, response));
    }
}
