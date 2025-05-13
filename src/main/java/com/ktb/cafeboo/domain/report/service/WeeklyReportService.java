package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.report.dto.CoffeeTimeStats;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.repository.WeeklyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyReportRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyReportResponse;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final AiServerClient aiServerClient;

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
                    .monthlyStatisticsId(monthlyReport)
                    .year(year)
                    .month(month)
                    .weekNum(weekNum)
                    .totalCaffeineMg(0f)
                    .dailyCaffeineAvgMg(0f)
                    .overIntakeDays(0)
                    .aiMessage("주간 카페인 섭취 리포트 생성을 위해 1주간 섭취 내역을 등록해주세요!")
                    .build();

                // 새로 생성한 WeeklyReport를 데이터베이스에 저장
                return weeklyReportRepository.save(weeklyReport);
            });
    }

    public WeeklyCaffeineReportResponse getWeeklyReport(Long userId, String targetYear, String targetMonth, String targetWeek,  List<DailyStatistics> dailyStats, List<CaffeineIntake> intakes) {
        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);
        int week = Integer.parseInt(targetWeek);
        User user = userService.findUserById(userId);
        UserHealthInfo userHealthInfo = user.getHealthInfo();
        UserCaffeinInfo userCaffeinInfo = user.getCaffeinInfo();

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
        LocalDate endDate = startDate.plusDays(6);

        int isoWeekNum = startDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String isoWeek = String.format("%d-W%02d", year, isoWeekNum);

        WeeklyReport weeklyReport = weeklyReportRepository.findByUserIdAndYearAndWeekNum(userId, year, isoWeekNum)
            .orElseGet(() -> WeeklyReport.builder()
                .user(userService.findUserById(userId))
                .year(year)
                .month(month)
                .weekNum(week)
                .totalCaffeineMg(0f)
                .dailyCaffeineAvgMg(0f)
                .overIntakeDays(0)
                .aiMessage("주간 카페인 섭취 리포트 생성을 위해 1주간 섭취 내역을 등록해주세요!")
                .monthlyStatisticsId(null) // 필요시 null 또는 적절한 값
                .build());

        float weeklyTotal = weeklyReport.getTotalCaffeineMg();
        //float dailyLimit = user.getDailyLimit();
        int overLimitDays = weeklyReport.getOverIntakeDays();
        float dailyAvg = weeklyReport.getDailyCaffeineAvgMg();

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

        return WeeklyCaffeineReportResponse.builder()
            .filter(WeeklyCaffeineReportResponse.Filter.builder()
                .year(String.valueOf(year))
                .month(String.valueOf(month))
                .week(week + "주차")
                .build())
            .isoWeek(isoWeek)
            .startDate(startDate.toString())
            .endDate(endDate.toString())
            .weeklyCaffeineTotal(weeklyTotal)
            .dailyCaffeineLimit((int)userCaffeinInfo.getDailyCaffeineLimitMg())
            .overLimitDays(overLimitDays)
            .dailyCaffeineAvg(dailyAvg)
            .dailyIntakeTotals(dailyIntakeTotals)
            .aiMessage(weeklyReport.getAiMessage())
            .build();
    }

    public void updateWeeklyReport(Long userId, WeeklyReport weeklyReport, Float additionalCaffeine){
        User user = userService.findUserById(userId);

        // 2. 주간 리포트의 총 카페인 섭취량 업데이트
        float newTotalCaffeine = weeklyReport.getTotalCaffeineMg() + additionalCaffeine;
        weeklyReport.setTotalCaffeineMg(newTotalCaffeine);

        // 3. 일일 평균 카페인 섭취량 계산 (7일로 나눔)
        float dailyAverage = newTotalCaffeine / 7.0f;
        weeklyReport.setDailyCaffeineAvgMg(dailyAverage);

        // 4. overLimitDays 구하기
        List<DailyStatistics> dailyStatistics = weeklyReport.getDailyStatistics();
        int overIntakeDays = 0;
        float userCaffeineLimit = user.getCaffeinInfo().getDailyCaffeineLimitMg();

        for(DailyStatistics dailyStatistic : dailyStatistics){
            if(dailyStatistic.getTotalCaffeineMg() >= userCaffeineLimit)
                overIntakeDays++;
        }
        weeklyReport.setOverIntakeDays(overIntakeDays);

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

    public void saveReport(WeeklyReport weeklyReport){
        weeklyReportRepository.save(weeklyReport);
    }

    public void updateWeeklyReportAfterUpdate(Long userId, WeeklyReport weeklyReport){
        User user = userService.findUserById(userId);

        // 1. 바뀐 개인별 카페인 권장량을 바탕으로 overLimitDays 구하기
        List<DailyStatistics> dailyStatistics = weeklyReport.getDailyStatistics();
        int overIntakeDays = 0;
        float userCaffeineLimit = user.getCaffeinInfo().getDailyCaffeineLimitMg();

        for(DailyStatistics dailyStatistic : dailyStatistics){
            if(dailyStatistic.getTotalCaffeineMg() >= userCaffeineLimit)
                overIntakeDays++;
        }
        weeklyReport.setOverIntakeDays(overIntakeDays);

        weeklyReportRepository.save(weeklyReport);
    }
}
