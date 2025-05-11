package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportScheduler;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
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

        List<DailyStatistics> dailyStats = dailyStatisticsService.getDailyStatisticsForWeek(userId, targetYear, targetMonth, targetWeek);
        List<CaffeineIntake> intakes = intakeService.getDailyCaffeineIntakeForWeek(userId, targetYear, targetMonth, targetWeek);

        WeeklyCaffeineReportResponse response = weeklyReportService.getWeeklyReport(userId, targetYear, targetMonth, targetWeek, dailyStats, intakes);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS, response));
    }

    @GetMapping("/test")
    public void getWeeklyCaffeineReport(){
        weeklyReportScheduler.generateWeeklyReports();
    }
}
