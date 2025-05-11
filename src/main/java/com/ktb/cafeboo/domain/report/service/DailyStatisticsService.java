package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.model.YearlyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineResponse;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final AiServerClient aiServerClient;
    private final CaffeineResidualService caffeineResidualService;

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

        //Todo : 현재 카페인 양이 < limit이고 addtionalCaffeine을 더한 값이 limit 보다 크다면 weeklyReport의 overIntakeDays + 1
        //Todo : 현재 카페인 양이 > limit이고 addtionalCaffeine을 뺀 값이 limit 보다 크다면 weeklyReport의 overIntakeDays - 1
        float userDailyLimit = user.getCaffeinInfo().getDailyCaffeineLimitMg();
        float currentCaffeine = statistics.getTotalCaffeineMg();

        if(currentCaffeine < userDailyLimit && currentCaffeine + additionalCaffeine >= userDailyLimit){
            weeklyReport.setOverIntakeDays(weeklyReport.getOverIntakeDays() + 1);
        }
        else if(currentCaffeine >= userDailyLimit && currentCaffeine + additionalCaffeine < userDailyLimit){
            weeklyReport.setOverIntakeDays(weeklyReport.getOverIntakeDays() - 1);
        }

        statistics.setTotalCaffeineMg(statistics.getTotalCaffeineMg() + additionalCaffeine);

        PredictCanIntakeCaffeineRequest request = PredictCanIntakeCaffeineRequest.builder()
            .userId(user.getId().toString())
            .currentTime(convertTimeToFloat(LocalTime.now()))
            .sleepTime(convertTimeToFloat(user.getHealthInfo().getSleepTime()))
            .caffeineLimit(Math.round(user.getCaffeinInfo().getDailyCaffeineLimitMg()))
            .currentCaffeine(Math.round(statistics.getTotalCaffeineMg()))
            .caffeineSensitivity(user.getCaffeinInfo().getCaffeineSensitivity())
            .targetResidualAtSleep(50f)
            .residualAtSleep(residualAtSleep.getResidueAmountMg())
            .gender(user.getHealthInfo().getGender())
            .age(user.getHealthInfo().getAge())
            .weight(user.getHealthInfo().getWeight())
            .height(user.getHealthInfo().getHeight())
            .isSmoker(user.getHealthInfo().getSmoking() ? 1 : 0)
            .takeHormonalContraceptive(user.getHealthInfo().getTakingBirthPill() ? 1 : 0)
            .build();

        PredictCanIntakeCaffeineResponse response = aiServerClient.predictCanIntakeCaffeine(request);

        String message = "권장량의 " + (statistics.getTotalCaffeineMg() / user.getCaffeinInfo().getDailyCaffeineLimitMg()) * 100 + "%를 섭취 중이에요.";

        if(Objects.equals(response.getStatus(), "success")){
            if(Objects.equals(response.getData().getCaffeineStatus(), "N")){
                message += " 지금 카페인을 추가로 섭취하면 수면에 영향을 줄 수 있어요.";
            }
            else if (Objects.equals(response.getData().getCaffeineStatus(), "Y")){
                message += " 카페인을 추가로 섭취해도 수면에 영향이 없어요.";
            }
        }

        statistics.setAiMessage(message);

        DailyStatistics savedStatistics = dailyStatisticsRepository.save(statistics);

        weeklyReportService.updateWeeklyReport(user.getId(), weeklyReport, additionalCaffeine);
        monthlyReportService.updateMonthlyReport(user.getId(), monthlyReport, additionalCaffeine);
        yearlyReportService.updateYearlyReport(user.getId(), yearlyReport, additionalCaffeine);
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
            .aiMessage("아직 카페인 섭취 내역이 없어요. 카페인을 섭취해도 문제 없을 거 같네요")
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
                    .aiMessage("아직 카페인 섭취 내역이 없네요. 카페인을 섭취해도 수면에 영향이 없어요.")
                    .build();
            });
    }

    public List<DailyStatistics> getDailyStatisticsForWeek(Long userId, LocalDate targetDate){
        LocalDate startOfWeek = targetDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<DailyStatistics> stats = dailyStatisticsRepository.findByUserIdAndDateBetween(userId, startOfWeek, endOfWeek);

        Map<LocalDate, DailyStatistics> statsMap = stats.stream()
            .collect(Collectors.toMap(DailyStatistics::getDate, Function.identity()));

        User user = userService.findUserById(userId);

        // 4. 7일치 리스트 생성 (없으면 0mg으로 생성)
        List<DailyStatistics> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
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

    private static float convertTimeToFloat(LocalTime time) {
        if (time == null) {
            return 0.0f; // 또는 다른 적절한 기본값
        }
        return time.getHour() + (float) time.getMinute() / 60.0f;
    }
}
