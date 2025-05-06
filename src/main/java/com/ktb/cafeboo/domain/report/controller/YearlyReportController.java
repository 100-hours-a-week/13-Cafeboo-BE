package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.MonthlyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.YearlyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.service.MonthlyReportService;
import com.ktb.cafeboo.global.ApiResponse;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports/yearly")
public class YearlyReportController {
    private final MonthlyReportService monthReportService;

    @GetMapping
    ResponseEntity<ApiResponse<YearlyCaffeineReportResponse>> getYearlyCaffeineReport(
        @RequestParam(required = false) Year targetYear) {
        int year = targetYear != null ? targetYear.getValue() : Year.now().getValue();
        List<MonthlyReport> monthlyReports = monthReportService.getMonthlyReportForYear(2L, targetYear);

        List<YearlyCaffeineReportResponse.MonthlyIntakeTotal> monthlyIntakeTotals = monthlyReports.stream()
            .map(report -> YearlyCaffeineReportResponse.MonthlyIntakeTotal.builder()
                .month(report.getMonth())
                .totalCaffeineMg(report.getTotalCaffeineMg())
                .build())
            .collect(Collectors.toList());

        float yearlyCaffeineTotal = (float) monthlyIntakeTotals.stream()
            .mapToDouble(YearlyCaffeineReportResponse.MonthlyIntakeTotal::getTotalCaffeineMg)
            .sum();

        float monthlyCaffeineAvg = monthlyIntakeTotals.isEmpty() ? 0f :
            yearlyCaffeineTotal / monthlyIntakeTotals.size();

        String startDate = year + "-01-01";
        String endDate = year + "-12-31";

        YearlyCaffeineReportResponse response =  YearlyCaffeineReportResponse.builder()
            .filter(YearlyCaffeineReportResponse.Filter.builder().year(String.valueOf(year)).build())
            .startDate(startDate)
            .endDate(endDate)
            .yearlyCaffeineTotal(yearlyCaffeineTotal)
            .monthlyCaffeineAvg(monthlyCaffeineAvg)
            .monthlyIntakeTotals(monthlyIntakeTotals)
            .summaryMessage("")
            .build();

        return ResponseEntity.ok(ApiResponse.<com.ktb.cafeboo.domain.report.dto.YearlyCaffeineReportResponse>builder()
            .status(200)
            .code("YEARLY_CAFFEINE_REPORT_SUCCESS")
            .message("연간 카페인 리포트를 성공적으로 조회했습니다.")
            .data(response)
            .build());
    }
}
