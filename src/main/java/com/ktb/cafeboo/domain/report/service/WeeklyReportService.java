package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.report.dto.MonthlyCaffeineReportResponse;
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
import com.ktb.cafeboo.global.infra.ai.dto.ReceiveWeeklyAnalysisRequest;
import jakarta.transaction.Transactional;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    public WeeklyCaffeineReportResponse getWeeklyReport(Long userId, String targetYear, String targetMonth, String targetWeek,  List<DailyStatistics> dailyStats) {
        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);
        int week = Integer.parseInt(targetWeek);

        User user = userService.findUserById(userId);
        UserCaffeinInfo userCaffeinInfo = user.getCaffeinInfo();

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

        LocalDate startDate = startOfMonth.plusWeeks(week - 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
        int overLimitDays = weeklyReport.getOverIntakeDays();
        float dailyAvg = weeklyReport.getDailyCaffeineAvgMg();

        List<WeeklyCaffeineReportResponse.DailyIntakeTotal> dailyIntakeTotals = dailyStats.stream()
            .map(stat -> new WeeklyCaffeineReportResponse.DailyIntakeTotal(
                    stat.getDate().toString(),
                    Math.round(stat.getTotalCaffeineMg())
            ))
            .collect(Collectors.toList());

        for (int i = 0; i < 7; i++) {
            LocalDate d = startDate.plusDays(i);
            boolean exists = dailyIntakeTotals.stream().anyMatch(t -> t.date().equals(d.toString()));
            if (!exists) {
                dailyIntakeTotals.add(
                    new WeeklyCaffeineReportResponse.DailyIntakeTotal(
                            d.toString(),
                            0
                    )
                );
            }
        }

        return new WeeklyCaffeineReportResponse(
            new WeeklyCaffeineReportResponse.Filter(
                    String.valueOf(year),
                    String.valueOf(month),
                    week + "주차"
            ),
            isoWeek,
            startDate.toString(),
            endDate.toString(),
            weeklyTotal,
            (int) userCaffeinInfo.getDailyCaffeineLimitMg(),
            overLimitDays,
            dailyAvg,
            dailyIntakeTotals,
            weeklyReport.getAiMessage()
        );
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

        if(dailyStatistics != null){
            for(DailyStatistics dailyStatistic : dailyStatistics){
                if(dailyStatistic.getTotalCaffeineMg() >= userCaffeineLimit)
                    overIntakeDays++;
            }
        }
        weeklyReport.setOverIntakeDays(overIntakeDays);

        weeklyReportRepository.save(weeklyReport);
    }

    public MonthlyCaffeineReportResponse getWeeklyStatisticsForMonth(Long userId, String givenYear, String givenMonth){
        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(Integer.parseInt(givenYear), Integer.parseInt(givenMonth));
        }
        catch (Exception e) {
            throw new CustomApiException(ErrorStatus.INVALID_PARAMETER);
        }


        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        User user = userService.findUserById(userId);

        // 1. 해당 월의 모든 주차(ISO 기준) 구하기
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        List<WeeklyReport> weeklyStats = weeklyReportRepository.findByUserIdAndYearAndMonth(userId, year, month);

        // 3. (year, week) → WeeklyReport Map
        Map<String, WeeklyReport> weeklyReportMaps = weeklyStats.stream()
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
            WeeklyReport report = weeklyReportMaps.get(key);

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

        int resolvedYear = yearMonth.getYear();
        int resolvedMonth = yearMonth.getMonthValue();

        startOfMonth = yearMonth.atDay(1);
        LocalDate startDate = startOfMonth;
        DayOfWeek dayOfWeek = startOfMonth.getDayOfWeek();

        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
        if (dayOfWeek == DayOfWeek.FRIDAY ||
            dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            startOfMonth = startOfMonth.plusWeeks(1);
        }

        endOfMonth = yearMonth.atEndOfMonth();
        LocalDate endDate = endOfMonth;
        dayOfWeek = endOfMonth.getDayOfWeek();

        //ISO 8601 기준은 월요일 기준. 월 ~ 일요일 까지 날짜 중, 과반 수 이상이 포함된 주차로 속하게 됨
        if (dayOfWeek == DayOfWeek.MONDAY ||
            dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.WEDNESDAY) {
            endOfMonth = endOfMonth.minusWeeks(1);
        }


        Set<Integer> weekNums = new TreeSet<>();
        cursor = startOfMonth.with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(endOfMonth)) {
            weekNums.add(cursor.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            cursor = cursor.plusWeeks(1);
        }

        Map<Integer, WeeklyReport> reportMap = weeklyStats.stream()
            .collect(Collectors.toMap(WeeklyReport::getWeekNum, Function.identity()));

        MonthlyCaffeineReportResponse response = MonthlyCaffeineReportResponse.create(
            resolvedYear, resolvedMonth, startDate, endDate, reportMap, weekNums
        );

        return response;
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

        if(dailyStatistics != null){
            for(DailyStatistics dailyStatistic : dailyStatistics){
                if(dailyStatistic.getTotalCaffeineMg() >= userCaffeineLimit)
                    overIntakeDays++;
            }
        }
        weeklyReport.setOverIntakeDays(overIntakeDays);

        weeklyReportRepository.save(weeklyReport);
    }

    public void updateAiMessage(List<ReceiveWeeklyAnalysisRequest.ReportDto> receivedReports){
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int weekNum = yesterday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = yesterday.getYear();

        for(ReceiveWeeklyAnalysisRequest.ReportDto report : receivedReports) {
            Long userId = Long.valueOf(report.getUserId());
            String WeeklyReportAnalysis = report.getReport();

            WeeklyReport weeklyReport = weeklyReportRepository.findByUserIdAndYearAndWeekNum(userId, year, weekNum)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.REPORT_NOT_FOUND));

            weeklyReport.setAiMessage(WeeklyReportAnalysis);

            weeklyReportRepository.save(weeklyReport);
        }
    }
}
