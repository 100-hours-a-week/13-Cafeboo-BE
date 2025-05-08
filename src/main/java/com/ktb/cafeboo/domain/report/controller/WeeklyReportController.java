package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
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
@RequestMapping("/api/v1/reports/weekly")
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;
    private final DailyStatisticsService dailyStatisticsService;

    /**
     * 사용자의 일일 카페인 섭취 현황 데이터를 조회합니다.
     * 상세 스펙은 @see <a href="https://freckle-pipe-840.notion.site/1ddb43be904c80ccbb02c746ff16a3ba">주간 섭취 현황 조회 API 스펙 문서</a>
     //     * @param user 현재 인증된 사용자
     * @param targetDate 조회할 날짜 (yyyy-MM-dd), 미입력시 오늘 날짜
     * @return 일일 카페인 섭취 리포트
     * @throws IllegalArgumentException 잘못된 요청 파라미터
    //     * @throws AuthenticationException 인증 실패
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeeklyCaffeineReportResponse>> getWeeklyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) LocalDate targetDate) {

        int year = targetDate.get(IsoFields.WEEK_BASED_YEAR);
        int weekNum = targetDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int month = targetDate.getMonthValue();
        LocalDate startDate = targetDate.with(DayOfWeek.MONDAY);
        LocalDate endDate = startDate.plusDays(6);

        String isoWeek = String.format("%d-W%02d", year, weekNum);

        Long userId = userDetails.getId();

        WeeklyReport weeklyReport = weeklyReportService.getWeeklyReport(userId, targetDate);
        float weeklyTotal = weeklyReport.getTotalCaffeineMg();
        //float dailyLimit = user.getDailyLimit();
        int overLimitDays = weeklyReport.getOverIntakeDays();
        float dailyAvg = weeklyReport.getDailyCaffeineAvgMg();

        List<DailyStatistics> dailyStats = dailyStatisticsService.getDailyStatisticsForWeek(userId, targetDate);

        List<WeeklyCaffeineReportResponse.DailyIntakeTotal> dailyIntakeTotals = dailyStats.stream()
            .map(stat -> WeeklyCaffeineReportResponse.DailyIntakeTotal.builder()
                .date(stat.getDate().toString())
                .caffeineMg(Math.round(stat.getTotalCaffeineMg()))
                .build())
            .collect(Collectors.toList());

        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            boolean exists = dailyIntakeTotals.stream().anyMatch(t -> t.getDate().equals(d.toString()));
            if (!exists) {
                dailyIntakeTotals.add(
                    WeeklyCaffeineReportResponse.DailyIntakeTotal.builder()
                        .date(d.toString())
                        .caffeineMg(0)
                        .build()
                );
            }
        }

        String summaryMessage = "이번 주 평균 섭취량은 권장량의 " + (dailyAvg * 100 / 400) + "% 수준입니다.";

        WeeklyCaffeineReportResponse response = WeeklyCaffeineReportResponse.builder()
            .filter(WeeklyCaffeineReportResponse.Filter.builder()
                .year(String.valueOf(year))
                .month(String.valueOf(month))
                .week(weekNum + "주차")
                .build())
            .isoWeek(isoWeek)
            .startDate(startDate.toString())
            .endDate(endDate.toString())
            .weeklyCaffeineTotal(weeklyTotal)
            .dailyCaffeineLimit(400)
            .overLimitDays(overLimitDays)
            .dailyCaffeineAvg(dailyAvg)
            .dailyIntakeTotals(dailyIntakeTotals)
            .summaryMessage(summaryMessage)
            .build();

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS, response));
    }
}
