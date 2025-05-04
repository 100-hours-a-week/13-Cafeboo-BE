package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.report.repository.WeeklyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserService userService;

    public WeeklyReport getWeeklyReport(Long userId, LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int month = date.getMonthValue();

        return weeklyReportRepository
            .findByUserIdAndYearAndWeekNum(userId, year, weekNum)
            .orElseGet(() -> {
                User user = userService.findUserById(userId);
                WeeklyReport weeklyReport = WeeklyReport.builder()
                    .user(user)
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
}
