package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.model.YearlyReport;
import com.ktb.cafeboo.domain.report.repository.YearlyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class YearlyReportService {
    private final UserService userService;
    private final YearlyReportRepository yearlyReportRepository;

    public YearlyReport getOrCreateYearlyReport(Long userId, LocalDate date){
        User user = userService.findUserById(userId);

        int year = date.getYear();
        Optional<YearlyReport> existingReport = yearlyReportRepository.findByYearAndUserId(year, userId);

        return existingReport.orElseGet(() -> {
            YearlyReport newReport = YearlyReport.builder()
                .year(year)
                .totalCaffeineMg(0.0f) // 초기값 0으로 설정
                .monthlyCaffeineAvgMg(0.0f) // 초기값 0으로 설정
                .user(user)
                .build();
            return yearlyReportRepository.save(newReport);
        });
    }

    public void updateYearlyReport(Long userId, YearlyReport yearlyReport, Float additionalCaffeine) {
        User user = userService.findUserById(userId);

        // 2. 연간 리포트의 총 카페인 섭취량 업데이트
        float newTotalCaffeine = yearlyReport.getTotalCaffeineMg() + additionalCaffeine;
        yearlyReport.setTotalCaffeineMg(newTotalCaffeine);

        // 3. 월간 평균 카페인 섭취량 계산 (7일로 나눔)
        float monthlyAverage = newTotalCaffeine / 12f;
        yearlyReport.setMonthlyCaffeineAvgMg(monthlyAverage);
    }
}
