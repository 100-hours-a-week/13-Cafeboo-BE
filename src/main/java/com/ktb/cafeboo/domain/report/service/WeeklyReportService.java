package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.report.repository.WeeklyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
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
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserService userService;

    public WeeklyReport getOrCreateWeeklyReport(Long userId, MonthlyReport monthlyReport, LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int month = date.getMonthValue();

        return weeklyReportRepository
            .findByUserIdAndYearAndWeekNum(userId, year, weekNum)
            .orElseGet(() -> {
                User user = userService.findUserById(userId);
                WeeklyReport weeklyReport = WeeklyReport.builder()
                    .user(user)
                    .monthlyReport(monthlyReport)
                    .year(year)
                    .month(month)
                    .weekNum(weekNum)
                    .totalCaffeineMg(0f)
                    .dailyCaffeineAvgMg(0f)
                    .overIntakeDays(0)
                    .build();

                // 새로 생성한 WeeklyReport를 데이터베이스에 저장
                return weeklyReportRepository.save(weeklyReport);
            });
    }

    public WeeklyReport getWeeklyReport(Long userId, LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int month = date.getMonthValue();

        return weeklyReportRepository.findByUserIdAndYearAndWeekNum(userId, year, weekNum)
            .orElse(null);
    }

    public void updateWeeklyReport(Long userId, WeeklyReport weeklyReport, Float additionalCaffeine){
        User user = userService.findUserById(userId);

        // 2. 주간 리포트의 총 카페인 섭취량 업데이트
        float newTotalCaffeine = weeklyReport.getTotalCaffeineMg() + additionalCaffeine;
        weeklyReport.setTotalCaffeineMg(newTotalCaffeine);

        // 3. 일일 평균 카페인 섭취량 계산 (7일로 나눔)
        float dailyAverage = newTotalCaffeine / 7.0f;
        weeklyReport.setDailyCaffeineAvgMg(dailyAverage);

        // 4. 허용치 초과 여부 판단 및 overIntakeDays 증가
        Float caffeineLimit = 400F; //
        if (dailyAverage > caffeineLimit) {
            // 기존 overIntakeDays 값이 null일 수 있으니 0으로 처리
            int overIntakeDays = weeklyReport.getOverIntakeDays() != null ? weeklyReport.getOverIntakeDays() : 0;
            weeklyReport.setOverIntakeDays(overIntakeDays + 1);
        }

        weeklyReportRepository.save(weeklyReport);
    }

    public List<WeeklyReport> getWeeklyStatisticsForMonth(Long userId, YearMonth yearMonth){
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        User user = userService.findUserById(userId);

        // 1. 해당 월의 모든 주차(ISO 기준) 구하기
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // ISO 기준 주차/연도
        int startWeek = startOfMonth.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int endWeek = endOfMonth.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        int startYear = startOfMonth.get(IsoFields.WEEK_BASED_YEAR);
        int endYear = endOfMonth.get(IsoFields.WEEK_BASED_YEAR);

        List<WeeklyReport> weeklyStats = weeklyReportRepository.findByUserIdAndYearAndMonth(userId, year, month);

        // 3. (year, week) → WeeklyReport Map
        Map<String, WeeklyReport> reportMap = weeklyStats.stream()
            .collect(Collectors.toMap(
                r -> r.getYear() + "-" + r.getWeekNum(),
                Function.identity()
            ));

        List<WeeklyReport> result = new ArrayList<>();

        // 4. 월의 모든 주차에 대해 루프
        LocalDate cursor = startOfMonth.with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(endOfMonth)) {
            int weekNum = cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int weekYear = cursor.get(IsoFields.WEEK_BASED_YEAR);

            String key = weekYear + "-" + weekNum;
            WeeklyReport report = reportMap.get(key);

            if (report != null) {
                result.add(report);
            } else {
                // 없는 경우 0으로 채운 WeeklyReport 생성
                WeeklyReport newReport = new WeeklyReport();
                newReport.setUser(user);
                newReport.setYear(weekYear);
                newReport.setMonth(month);
                newReport.setWeekNum(weekNum);
                newReport.setTotalCaffeineMg(0f);
                newReport.setDailyCaffeineAvgMg(0f);
                newReport.setOverIntakeDays(0);
                result.add(newReport);
            }
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }
}
