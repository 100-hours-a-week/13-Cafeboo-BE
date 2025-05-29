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

        MonthlyCaffeineReportResponse response = weeklyReportService.getWeeklyStatisticsForMonth(userId, year, month);
//        int resolvedYear = targetMonth.getYear();
//        int resolvedMonth = targetMonth.getMonthValue();
//
//        LocalDate startOfMonth = targetMonth.atDay(1);
//        LocalDate startDate = startOfMonth;
//        DayOfWeek dayOfWeek = startOfMonth.getDayOfWeek();
//
//        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
//        if (dayOfWeek == DayOfWeek.FRIDAY ||
//            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
//            startOfMonth = startOfMonth.plusWeeks(1);
//        }
//
//        LocalDate endOfMonth = targetMonth.atEndOfMonth();
//        LocalDate endDate = endOfMonth;
//        dayOfWeek = endOfMonth.getDayOfWeek();
//
//        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
//        if (dayOfWeek == DayOfWeek.MONDAY ||
//            dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.WEDNESDAY) {
//            endOfMonth = endOfMonth.minusWeeks(1);
//        }
//
//
//        Set<Integer> weekNums = new TreeSet<>();
//        LocalDate cursor = startOfMonth.with(DayOfWeek.MONDAY);
//        while (!cursor.isAfter(endOfMonth)) {
//            weekNums.add(cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
//            cursor = cursor.plusWeeks(1);
//        }
//
//        Map<Integer, WeeklyReport> reportMap = weeklyStats.stream()
//            .collect(Collectors.toMap(WeeklyReport::getWeekNum, Function.identity()));
//
//        List<MonthlyCaffeineReportResponse.weeklyIntakeTotal> weeklyIntakeTotals = weekNums.stream()
//            .map(weekNum -> {
//                WeeklyReport report = reportMap.get(weekNum);
//                if (report != null) {
//                    return MonthlyCaffeineReportResponse.weeklyIntakeTotal.builder()
//                        .isoWeek(String.format("%d-W%02d", report.getYear(), report.getWeekNum()))
//                        .totalCaffeineMg(Math.round(report.getTotalCaffeineMg()))
//                        .build();
//                } else {
//                    // 없는 주차는 0으로 채움
//                    return MonthlyCaffeineReportResponse.weeklyIntakeTotal.builder()
//                        .isoWeek(String.format("%d-W%02d", resolvedYear, weekNum))
//                        .totalCaffeineMg(0)
//                        .build();
//                }
//            })
//            .collect(Collectors.toList());
//
//        float sum = (float) weeklyIntakeTotals.stream()
//            .mapToDouble(MonthlyCaffeineReportResponse.weeklyIntakeTotal::getTotalCaffeineMg)
//            .sum();
//
//        float avg = weeklyIntakeTotals.isEmpty() ? 0f : sum / weeklyIntakeTotals.size();
//
//        MonthlyCaffeineReportResponse response = MonthlyCaffeineReportResponse.builder()
//            .filter(MonthlyCaffeineReportResponse.Filter.builder()
//                .year(String.valueOf(resolvedYear))
//                .month(String.valueOf(resolvedMonth))
//                .build()
//            )
//            .startDate(startDate.toString())
//            .endDate(endDate.toString())
//            .monthlyCaffeineTotal(sum)
//            .weeklyCaffeineAvg(avg)
//            .weeklyIntakeTotals(weeklyIntakeTotals)
//            .build();

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.MONTHLY_CAFFEINE_REPORT_SUCCESS, response));
    }
}
