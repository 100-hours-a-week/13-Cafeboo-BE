package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.model.MonthlyReport;
import com.ktb.cafeboo.domain.report.model.YearlyReport;
import com.ktb.cafeboo.domain.report.repository.MonthlyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Year;
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
public class MonthlyReportService {
    private final MonthlyReportRepository monthlyReportRepository;
    private final UserService userService;

    public MonthlyReport getOrCreateMonthlyReport(Long userId, YearlyReport yearlyReport, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        User user = userService.findUserById(userId);

        return monthlyReportRepository
            .findByUserIdAndYearAndMonth(userId, year, month)
            .orElseGet(() -> {
                MonthlyReport newReport = MonthlyReport.builder()
                    .user(user)
                    .year(year)
                    .month(month)
                    .yearlyStatisticsId(yearlyReport)
                    .totalCaffeineMg(0f)
                    .weeklyCaffeineAvgMg(0f)
                    .aiMessage("")
                    .build();
                return monthlyReportRepository.save(newReport);
            });
    }

    public List<MonthlyReport> getMonthlyReportForYear(Long userId, Year targetYear) {
        int year = targetYear != null ? targetYear.getValue() : Year.now().getValue();
        List<MonthlyReport> monthlyReports = monthlyReportRepository.findByUserIdAndYear(userId, year);

        Map<Integer, MonthlyReport> reportMap = monthlyReports.stream()
            .collect(Collectors.toMap(MonthlyReport::getMonth, Function.identity()));

        List<MonthlyReport> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            MonthlyReport report = reportMap.get(month);
            if (report != null) {
                result.add(report);
            } else {
                // 없는 월은 0으로 채운 MonthlyReport 생성
                result.add(MonthlyReport.builder()
                    .user(userService.findUserById(userId))
                    .year(year)
                    .month(month)
                    .totalCaffeineMg(0f)
                    .weeklyCaffeineAvgMg(0f)
                    .build());
            }
        }
        return result;
    }

    public void updateMonthlyReport(Long userId, MonthlyReport monthlyReport, Float additionalCaffeine) {
        User user = userService.findUserById(userId);

        int month = monthlyReport.getMonth();

        // 2. 연간 리포트의 총 카페인 섭취량 업데이트
        float newTotalCaffeine = monthlyReport.getTotalCaffeineMg() + additionalCaffeine;
        monthlyReport.setTotalCaffeineMg(newTotalCaffeine);

        // 3. 월간 평균 카페인 섭취량 계산 (7일로 나눔)
        float weeklyAverage = newTotalCaffeine / 4f;
        monthlyReport.setWeeklyCaffeineAvgMg(weeklyAverage);
    }
}
