package com.ktb.cafeboo.domain.report.controller;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportScheduler;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisResponse;
import com.ktb.cafeboo.global.infra.ai.dto.ReceiveWeeklyAnalysisRequest;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/reports/weekly")
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;
    private final DailyStatisticsService dailyStatisticsService;
    private final CaffeineIntakeService intakeService;
    private final WeeklyReportScheduler weeklyReportScheduler;
    private final UserService userService;
    /**
     * 사용자의 주간 카페인 섭취 현황 데이터를 조회합니다.
     * 상세 스펙은 @see <a href="https://freckle-pipe-840.notion.site/1ddb43be904c80ccbb02c746ff16a3ba">주간 섭취 현황 조회 API 스펙 문서</a>
     //     * @param user 현재 인증된 사용자
     * @return 주간 카페인 섭취 리포트
     * @throws IllegalArgumentException 잘못된 요청 파라미터
    //     * @throws AuthenticationException 인증 실패
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WeeklyCaffeineReportResponse>> getWeeklyCaffeineReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(name = "year", required = false) String targetYear,
        @RequestParam(name ="month", required = false) String targetMonth,
        @RequestParam(name = "week",required = false) String targetWeek) {
        log.info("[WeeklyReportController.getWeeklyCaffeineReport] 주간 카페인 리포트 요청 수신 - year={}, month={}, week={}", targetYear, targetMonth, targetWeek);

        Long userId = userDetails.getId();

        List<DailyStatistics> dailyStats = dailyStatisticsService.getDailyStatisticsForWeek(userId, targetYear, targetMonth, targetWeek);

        WeeklyCaffeineReportResponse response = weeklyReportService.getWeeklyReport(userId, targetYear, targetMonth, targetWeek, dailyStats);

        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.WEEKLY_CAFFEINE_REPORT_SUCCESS, response));
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<CreateWeeklyAnalysisResponse>> sendWeeklyCaffeineReportToAI(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        log.info("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 주간 리포트 AI 생성 요청 시작");
        try {
            log.debug("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 주간 리포트 생성 스케줄러 호출");
            CreateWeeklyAnalysisResponse response = weeklyReportScheduler.generateWeeklyReports();
            log.debug("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 주간 리포트 생성 완료 - 응답={}", response);
            return ResponseEntity.ok(ApiResponse.of(SuccessStatus.REPORT_GENERATION_SUCCESS, response));
        } catch (CustomApiException e) {
            log.warn("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 커스텀 예외 발생 - message={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 시스템 예외 발생 - message={}", e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.REPORT_GENERATION_FAILED);
        } finally {
            log.info("[WeeklyReportController.sendWeeklyCaffeineReportToAI] 주간 리포트 AI 생성 요청 종료");
        }
    }

    /**
     * AI 서버가 비동기로 유저 주간 섭취내역에 대한 평가 생성 완료 후 콜백으로 호출하는 BE 엔드포인트
     * @param request AI 서버가 비동기로 생성한 유저 주간 섭취내역에 대한 평가
     */
    @PostMapping("/ai_callback")
    public void getWeeklyCaffeineReportFromAI(@RequestBody ReceiveWeeklyAnalysisRequest request){
        log.info("[WeeklyReportController.getWeeklyCaffeineReportFromAI] AI 서버로부터 주간 리포트 분석 콜백 수신 - reportCount={}", request.getReports().size());

        List<ReceiveWeeklyAnalysisRequest.ReportDto> receivedReports = request.getReports();

        for(ReceiveWeeklyAnalysisRequest.ReportDto report : receivedReports){
            Long userId = Long.valueOf(report.getUserId());
            String WeeklyReportAnalysis = report.getReport();

            weeklyReportService.updateAiMessage(userId, WeeklyReportAnalysis);
        }
        log.info("[WeeklyReportController.getWeeklyCaffeineReportFromAI] AI 분석 처리 완료");
    }
}
