package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.model.YearlyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    /**
     * 일일 통계 데이터를 갱신합니다. 섭취 내역이 추가됨에 따라 일일 섭취 카페인 수치를 갱신합니다.
     * @param user 일일 통계를 기록할 유저 정보
     * @param date 섭취 내역을 등록/수정한 시점의 연-월-일 정보
     * @param additionalCaffeine 섭취 내역이 추가/변경됨에 따라 변경될 카페인 수치
     */
    public void updateDailyStatistics(User user, LocalDate date, float additionalCaffeine) {
        //우선은 동기적으로 구현 진행
        YearlyReport yearlyReport = yearlyReportService.getOrCreateYearlyReport(user.getId(), date);
        yearlyReportService.updateYearlyReport(user.getId(), yearlyReport, additionalCaffeine);

        MonthlyReport monthlyReport = monthlyReportService.getOrCreateMonthlyReport(user.getId(), yearlyReport, date);
        monthlyReportService.updateMonthlyReport(user.getId(), monthlyReport, additionalCaffeine);

        WeeklyReport weeklyReport = weeklyReportService.getOrCreateWeeklyReport(user.getId(), monthlyReport, date);
        weeklyReportService.updateWeeklyReport(user.getId(), weeklyReport, additionalCaffeine);

        DailyStatistics statistics = dailyStatisticsRepository
            .findByUserIdAndDate(user.getId(), date)
            .orElseGet(() -> createDailyStatistics(user, weeklyReport, date));

        statistics.setTotalCaffeineMg(statistics.getTotalCaffeineMg() + additionalCaffeine);
        DailyStatistics savedStatistics = dailyStatisticsRepository.save(statistics);
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
}
