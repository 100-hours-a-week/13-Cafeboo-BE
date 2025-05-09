package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse.HourlyCaffeineInfo;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineResponse;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DailyReportService {

    private final DailyStatisticsService dailyStatisticsService;
    private final CaffeineResidualService residualService;
    private final UserService userService;
    private final AiServerClient aiServerClient;
    /**
     * 일일 카페인 리포트를 생성합니다.
     */
    public DailyCaffeineReportResponse createDailyReport(Long userId, LocalDate targetDate,
        LocalTime targetTime) {
        LocalDateTime currentDateTime = (targetDate != null && targetTime != null) ?
            LocalDateTime.of(targetDate, targetTime) :
            LocalDateTime.now();

        User user = userService.findUserById(userId);

        // 일일 총 섭취량 조회
        DailyStatistics dailyStatistics = dailyStatisticsService.getDailyStatistics(userId, targetDate);

        // 잔여량 데이터 조회
        List<CaffeineResidual> residuals = residualService.getCaffeineResidualsByTimeRange(user,
            currentDateTime);


        return DailyCaffeineReportResponse.builder()
            .nickname(user.getNickname())
            .dailyCaffeineLimit(400F)
            .dailyCaffeineIntakeMg(dailyStatistics.getTotalCaffeineMg())
            .dailyCaffeineIntakeRate(calculateIntakeRate(dailyStatistics.getTotalCaffeineMg()))
            .intakeGuide(dailyStatistics.getAiMessage())
            .sleepSensitiveThreshold(100F)
            .caffeineByHour(createHourlyCaffeineInfo(residuals, currentDateTime))
            .build();
    }

    private int calculateIntakeRate(float dailyTotal) {
        return (int) ((dailyTotal / 400) * 100);
    }

    private List<HourlyCaffeineInfo> createHourlyCaffeineInfo(
        List<CaffeineResidual> residuals,
        LocalDateTime currentDateTime) {

        List<HourlyCaffeineInfo> hourlyInfo = new ArrayList<>();
        LocalDateTime startTime = currentDateTime.minusHours(17).withMinute(0).withSecond(0).withNano(0);;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:00");

        for (CaffeineResidual residual : residuals) {
            float caffeineMg = calculateCaffeineForHour(residual, startTime);
            String timeInfo = timeFormatter.format(startTime);
            hourlyInfo.add(new HourlyCaffeineInfo(timeInfo, caffeineMg));
            startTime = startTime.plusHours(1);
        }
        return hourlyInfo;
    }

    private float calculateCaffeineForHour(CaffeineResidual residual,
        LocalDateTime timePoint) {
        float totalCaffeine = 0f;
        int count = 0;

        LocalDateTime residualTime = residual.getTargetDate().plusHours(residual.getHour());
        log.info("residualTime : {}, timePoint : {}\n", residualTime, timePoint);
        // 시간 비교를 단순화: 년, 월, 일, 시간 비교
        if (residualTime.isEqual(timePoint)) {
            log.info("Matching residual: timePoint={}, residualTime={}, amount={}", timePoint,
                residualTime, residual.getResidueAmountMg());
            totalCaffeine += residual.getResidueAmountMg();
            count++;
        } else {
            log.info("Not Matching residual: timePoint={}, residualTime={}, amount={}", timePoint,
                residualTime, residual.getResidueAmountMg());
        }
        return count > 0 ? totalCaffeine / count : 0f;
    }
}