package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportScheduler;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
    private final CaffeineIntakeService intakeService;
    private final WeeklyReportScheduler weeklyReportScheduler;
    /**
     * 사용자의 일일 카페인 섭취 현황 데이터를 조회합니다.
     * 상세 스펙은 @see <a href="https://freckle-pipe-840.notion.site/1ddb43be904c80ccbb02c746ff16a3ba">주간 섭취 현황 조회 API 스펙 문서</a>
     //     * @param user 현재 인증된 사용자
     * @return 일일 카페인 섭취 리포트
     * @throws IllegalArgumentException 잘못된 요청 파라미터
    //     * @throws AuthenticationException 인증 실패
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeeklyCaffeineReportResponse>> getWeeklyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(name = "year", required = false) String targetYear,
        @RequestParam(name ="month", required = false) String targetMonth,
        @RequestParam(name = "week",required = false) String targetWeek) {

        Long userId = userDetails.getId();
        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);
        int week = Integer.parseInt(targetWeek);

        // 주어진 year와 month로 해당 달의 첫 번째 날짜를 얻습니다.
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        // 해당 달의 첫 번째 주 월요일을 찾습니다.
        LocalDate firstMondayOfMonth = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

        // 만약 첫 번째 날짜가 월요일보다 앞선다면, 그 주는 이전 달의 마지막 주에 해당할 수 있습니다.
        // 이를 보정하기 위해 첫 번째 월요일이 없다면 해당 달의 1일로 시작하는 주를 기준으로 합니다.
        LocalDate firstWeekStart = firstMondayOfMonth.getMonthValue() != month ?
            firstDayOfMonth : firstMondayOfMonth;

        // 첫 번째 주 시작 날짜에 (weekOfMonth - 1) 주를 더하여 해당 월의 weekOfMonth 번째 주의 시작 날짜를 얻습니다.
        LocalDate startDate = firstWeekStart.plusWeeks(week - 1);
        List<DailyStatistics> dailyStats = dailyStatisticsService.getDailyStatisticsForWeek(userId, startDate);
        List<CaffeineIntake> intakes = intakeService.getDailyCaffeineIntakeForWeek(userId, startDate, startDate.plusDays(6));

        WeeklyCaffeineReportResponse response = weeklyReportService.getWeeklyReport(userId, targetYear, targetMonth, targetWeek, dailyStats, intakes);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS, response));
    }

    @GetMapping("/test")
    public void getWeeklyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails){
        weeklyReportScheduler.generateWeeklyReports();
    }
}
