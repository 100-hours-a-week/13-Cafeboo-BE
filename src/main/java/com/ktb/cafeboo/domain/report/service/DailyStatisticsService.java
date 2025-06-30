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

//        PredictCanIntakeCaffeineRequest request = PredictCanIntakeCaffeineRequest.builder()
//            .userId(user.getId().toString())
//            .currentTime(convertTimeToFloat(LocalTime.now()))
//            .sleepTime(convertTimeToFloat(user.getHealthInfo().getSleepTime()))
//            .caffeineLimit(Math.round(user.getCaffeinInfo().getDailyCaffeineLimitMg()))
//            .currentCaffeine(Math.round(statistics.getTotalCaffeineMg()))
//            .caffeineSensitivity(user.getCaffeinInfo().getCaffeineSensitivity())
//            .targetResidualAtSleep(50f)
//            .residualAtSleep(residualAtSleep.getResidueAmountMg())
//            .gender(user.getHealthInfo().getGender())
//            .age(user.getHealthInfo().getAge())
//            .weight(user.getHealthInfo().getWeight())
//            .height(user.getHealthInfo().getHeight())
//            .isSmoker(user.getHealthInfo().getSmoking() ? 1 : 0)
//            .takeHormonalContraceptive(user.getHealthInfo().getTakingBirthPill() ? 1 : 0)
//            .build();
//
//        PredictCanIntakeCaffeineResponse response = aiServerClient.predictCanIntakeCaffeine(request);
//
//        String message = "";
//
//        if(Objects.equals(response.getStatus(), "success")){
//            if(Objects.equals(response.getData().getCaffeineStatus(), "N")){
//                message += "카페인을 추가로 섭취하면 수면에 영향을 줄 수 있어요.";
//            }
//            else if (Objects.equals(response.getData().getCaffeineStatus(), "Y")){
//                message += "카페인을 추가로 섭취해도 수면에 영향이 없어요.";
//            }
//        }

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

//        // 주어진 year와 month로 해당 달의 첫 번째 날짜를 얻습니다.
//        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
//
//        // 해당 달의 첫 번째 주 월요일을 찾습니다.
//        LocalDate firstMondayOfMonth = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
//
//        // 만약 첫 번째 날짜가 월요일보다 앞선다면, 그 주는 이전 달의 마지막 주에 해당할 수 있습니다.
//        // 이를 보정하기 위해 첫 번째 월요일이 없다면 해당 달의 1일로 시작하는 주를 기준으로 합니다.
//        LocalDate firstWeekStart = firstMondayOfMonth.getMonthValue() != month ?
//            firstDayOfMonth : firstMondayOfMonth;
//
//        // 첫 번째 주 시작 날짜에 (weekOfMonth - 1) 주를 더하여 해당 월의 weekOfMonth 번째 주의 시작 날짜를 얻습니다.
//        LocalDate startDate = firstWeekStart.plusWeeks(week - 1);
//
//        LocalDate startOfWeek = startDate.with(DayOfWeek.MONDAY);
//        LocalDate endOfWeek = startOfWeek.plusDays(6);

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

//        // 주어진 year와 month로 해당 달의 첫 번째 날짜를 얻습니다.
//        LocalDate startOfMonth = LocalDate.of(year, month, 1);
//
//        // 해당 달의 첫 번째 주 월요일을 찾습니다.
//        LocalDate firstMondayOfMonth = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
//
//        // 만약 첫 번째 날짜가 월요일보다 앞선다면, 그 주는 이전 달의 마지막 주에 해당할 수 있습니다.
//        // 이를 보정하기 위해 첫 번째 월요일이 없다면 해당 달의 1일로 시작하는 주를 기준으로 합니다.
//        LocalDate firstWeekStart = firstMondayOfMonth.getMonthValue() != month ?
//            firstDayOfMonth : firstMondayOfMonth;
//
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
