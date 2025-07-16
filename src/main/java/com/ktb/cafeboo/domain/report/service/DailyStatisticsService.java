package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.ai.service.CaffeineRecommendationService;
import com.ktb.cafeboo.domain.ai.service.IntakeSuggestionService;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.model.YearlyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DailyStatisticsService {

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final WeeklyReportService weeklyReportService;
    private final MonthlyReportService monthlyReportService;
    private final YearlyReportService yearlyReportService;
    private final UserService userService;
    private final CaffeineResidualService caffeineResidualService;
    private final IntakeSuggestionService intakeSuggestionService;
    /**
     * 일일 통계 데이터를 갱신합니다. 섭취 내역이 추가됨에 따라 일일 섭취 카페인 수치를 갱신합니다.
     * @param user 일일 통계를 기록할 유저 정보
     * @param date 섭취 내역을 등록/수정한 시점의 연-월-일 정보
     * @param additionalCaffeine 섭취 내역이 추가/변경됨에 따라 변경될 카페인 수치
     */
    public void updateDailyStatistics(User user, LocalDate date, float additionalCaffeine) {
        //우선은 동기적으로 구현 진행
        YearlyReport yearlyReport = yearlyReportService.getOrCreateYearlyReport(user.getId(), date);

        MonthlyReport monthlyReport = monthlyReportService.getOrCreateMonthlyReport(user.getId(), yearlyReport, date);

        WeeklyReport weeklyReport = weeklyReportService.getOrCreateWeeklyReport(user.getId(), monthlyReport, date);

        DailyStatistics statistics = dailyStatisticsRepository
            .findByUserIdAndDate(user.getId(), date)
            .orElseGet(() -> createDailyStatistics(user, weeklyReport, date));

        CaffeineResidual residualAtSleep = caffeineResidualService.findByUserAndTargetDateAndHour(user, date.atStartOfDay(), user.getHealthInfo().getSleepTime().getHour());

        statistics.setTotalCaffeineMg(statistics.getTotalCaffeineMg() + additionalCaffeine);

        int currentCaffeine = Math.round(statistics.getTotalCaffeineMg());
        double caffeineResidualAtSleep = residualAtSleep.getResidueAmountMg();

        String message = intakeSuggestionService.getPredictedIntakeSuggestion(user, currentCaffeine, caffeineResidualAtSleep);

        statistics.setAiMessage(message);

        DailyStatistics savedStatistics = dailyStatisticsRepository.save(statistics);

        weeklyReportService.updateWeeklyReport(user.getId(), weeklyReport, additionalCaffeine);
        monthlyReportService.updateMonthlyReport(user.getId(), monthlyReport, additionalCaffeine);
        yearlyReportService.updateYearlyReport(user.getId(), yearlyReport, additionalCaffeine);
    }

    public void updateDailyStatisticsAfterUpdateUserInfo(User user, LocalDate date){
        YearlyReport yearlyReport = yearlyReportService.getOrCreateYearlyReport(user.getId(), date);

        MonthlyReport monthlyReport = monthlyReportService.getOrCreateMonthlyReport(user.getId(), yearlyReport, date);

        WeeklyReport weeklyReport = weeklyReportService.getOrCreateWeeklyReport(user.getId(), monthlyReport, date);

        DailyStatistics statistics = dailyStatisticsRepository
            .findByUserIdAndDate(user.getId(), date)
            .orElseGet(() -> createDailyStatistics(user, weeklyReport, date));

        CaffeineResidual residualAtSleep = caffeineResidualService.findByUserAndTargetDateAndHour(user, date.atStartOfDay(), user.getHealthInfo().getSleepTime().getHour());

        int currentCaffeine = Math.round(statistics.getTotalCaffeineMg());
        double caffeineResidualAtSleep = residualAtSleep.getResidueAmountMg();

        String message = intakeSuggestionService.getPredictedIntakeSuggestion(user, currentCaffeine, caffeineResidualAtSleep);

        statistics.setAiMessage(message);

        DailyStatistics savedStatistics = dailyStatisticsRepository.save(statistics);
        weeklyReportService.updateWeeklyReportAfterUpdate(user.getId(), weeklyReport);
    }

    /**
     * 일일 통계 데이터를 생성합니다.
     * @param user 일일 통계를 기록할 유저 정보
     * @param weeklyReport 일일 통계가 속할 주간 기록으로의 FK
     * @param date 섭취 내역을 등록한 시점의 연-월-일 정보
     */
    private DailyStatistics createDailyStatistics(User user, WeeklyReport weeklyReport, LocalDate date) {
        //일일 통계 데이터를 반환
        return DailyStatistics.builder()
            .user(user)
            .date(date)
            .totalCaffeineMg(0f)
            .weeklyStatisticsId(weeklyReport)
            .aiMessage("카페인을 추가로 섭취해도 수면에 영향이 없어요.")
            .build();
    }

    /**
     * 특정 날짜의 총 카페인 섭취량을 조회합니다.
     * @param userId 조회 대상의 고유키
     * @param date 조회할 날짜
     * @return 총 카페인 섭취량 (mg)
     */
    public float getTotalCaffeineForDate(Long userId, LocalDate date) {
        return dailyStatisticsRepository.findTotalCaffeineByDate(userId, date)
            .orElse(0f);
    }

    public DailyStatistics getDailyStatistics(Long userId, LocalDate targetDate) {
        User user = userService.findUserById(userId);
        return dailyStatisticsRepository.findByUserIdAndDate(userId, targetDate)
            .orElseGet(() -> {
                return DailyStatistics.builder()
                    .user(user)
                    .weeklyStatisticsId(null)
                    .date(targetDate)
                    .totalCaffeineMg(0.0f)
                    .aiMessage("카페인을 추가로 섭취해도 수면에 영향이 없어요.")
                    .build();
            });
    }

    public List<DailyStatistics> getDailyStatisticsForWeek(Long userId, String targetYear, String targetMonth, String targetWeek){
        log.info("[DailyStatistics.getDailyStatisticsForWeek] 호출 시작 - userID={}, targetYear={}, targetMonth={}, targetWeek={}", userId, targetYear, targetMonth, targetWeek);
        if(targetYear == null || targetMonth == null || targetWeek == null
            || targetYear.isEmpty() || targetMonth.isEmpty() || targetWeek.isEmpty())
        {
            log.error("[DailyStatistics.getDailyStatisticsForWeek] 실행 실패 - userID={}, targetYear={}, targetMonth={}, targetWeek={}", userId, targetYear, targetMonth, targetWeek);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);
        int week = Integer.parseInt(targetWeek);

        LocalDate startOfMonth = LocalDate.of(year, month, 1);;
        DayOfWeek dayOfWeek = startOfMonth.getDayOfWeek();

        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
        if (dayOfWeek == DayOfWeek.FRIDAY ||
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            startOfMonth = startOfMonth.plusWeeks(1);
        }

        LocalDate endOfMonth = LocalDate.of(year, month, 1);
        dayOfWeek = endOfMonth.getDayOfWeek();

        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
        if (dayOfWeek == DayOfWeek.MONDAY ||
            dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.WEDNESDAY) {
            endOfMonth = endOfMonth.minusWeeks(1);
        }

//        // 첫 번째 주 시작 날짜에 (weekOfMonth - 1) 주를 더하여 해당 월의 weekOfMonth 번째 주의 시작 날짜를 얻습니다.
        LocalDate startDate = startOfMonth.plusWeeks(week - 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = startDate.plusDays(6);

        List<DailyStatistics> stats = dailyStatisticsRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        Map<LocalDate, DailyStatistics> statsMap = stats.stream()
            .collect(Collectors.toMap(DailyStatistics::getDate, Function.identity()));

        User user = userService.findUserById(userId);

        // 4. 7일치 리스트 생성 (없으면 0mg으로 생성)
        List<DailyStatistics> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyStatistics stat = statsMap.get(date);
            if (stat != null) {
                result.add(stat);
            } else {
                // 없는 경우 0mg으로 새 객체 생성 (id, weekly_statistics_id 등은 null/0으로)
                result.add(DailyStatistics.builder()
                    .user(user)
                    .date(date)
                    .totalCaffeineMg(0f)
                    .build());
            }
        }

        return result;
    }
}
