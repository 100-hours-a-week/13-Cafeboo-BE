package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.MonthlyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
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
import java.time.ZoneId;
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
        @RequestParam(required = false, name = "year") String year,
        @RequestParam(required = false, name = "month") String month){

        Long userId = userDetails.getId();
        YearMonth targetMonth;

        // year와 month 파라미터가 제공되지 않은 경우 현재 달을 기본값으로 사용
        if (year == null || month == null) {
            targetMonth = YearMonth.now(ZoneId.systemDefault()); // 또는 ZoneId.systemDefault()
        } else {
            try {
                targetMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
            } catch (DateTimeException e) {
                return ResponseEntity.badRequest().body(ApiResponse.of(
                    ErrorStatus.BAD_REQUEST, null));
            }
        }

        List<WeeklyReport> weeklyStats = weeklyReportService.getWeeklyStatisticsForMonth(userId, targetMonth);
        int resolvedYear = targetMonth.getYear();
        int resolvedMonth = targetMonth.getMonthValue();

        LocalDate startOfMonth = targetMonth.atDay(1);
        LocalDate endOfMonth = targetMonth.atEndOfMonth();

        Set<Integer> weekNums = new TreeSet<>();
        LocalDate cursor = startOfMonth.with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(endOfMonth)) {
            weekNums.add(cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            cursor = cursor.plusWeeks(1);
        }

        Map<Integer, WeeklyReport> reportMap = weeklyStats.stream()
            .collect(Collectors.toMap(WeeklyReport::getWeekNum, Function.identity()));

        List<MonthlyCaffeineReportResponse.weeklyIntakeTotal> weeklyIntakeTotals = weekNums.stream()
            .map(weekNum -> {
                WeeklyReport report = reportMap.get(weekNum);
                if (report != null) {
                    return MonthlyCaffeineReportResponse.weeklyIntakeTotal.builder()
                        .isoWeek(String.format("%d-W%02d", report.getYear(), report.getWeekNum()))
                        .totalCaffeineMg(Math.round(report.getTotalCaffeineMg()))
                        .build();
                } else {
                    // 없는 주차는 0으로 채움
                    return MonthlyCaffeineReportResponse.weeklyIntakeTotal.builder()
                        .isoWeek(String.format("%d-W%02d", resolvedYear, weekNum))
                        .totalCaffeineMg(0)
                        .build();
                }
            })
            .collect(Collectors.toList());

        float sum = (float) weeklyIntakeTotals.stream()
            .mapToDouble(MonthlyCaffeineReportResponse.weeklyIntakeTotal::getTotalCaffeineMg)
            .sum();

        float avg = weeklyIntakeTotals.isEmpty() ? 0f : sum / weeklyIntakeTotals.size();

        MonthlyCaffeineReportResponse response = MonthlyCaffeineReportResponse.builder()
            .filter(MonthlyCaffeineReportResponse.Filter.builder()
                .year(String.valueOf(resolvedYear))
                .month(String.valueOf(resolvedMonth))
                .build()
            )
            .startDate(startOfMonth.toString())
            .endDate(endOfMonth.toString())
            .monthlyCaffeineTotal(sum)
            .weeklyCaffeineAvg(avg)
            .weeklyIntakeTotals(weeklyIntakeTotals)
            .build();

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.MONTHLY_CAFFEINE_REPORT_SUCCESS, response));
    }
}
