package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.dto.DailyCaffeineReportResponse.HourlyCaffeineInfo;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.mapper.DailyReportMapper;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    public DailyCaffeineReportResponse createDailyReport(Long userId, LocalDate targetDate, LocalTime targetTime) {

        LocalDateTime currentDateTime = getReportBaseDateTime(targetDate, targetTime);;

        User user = userService.findUserById(userId);

        // 일일 총 섭취량 조회
        DailyStatistics dailyStatistics = dailyStatisticsService.getDailyStatistics(userId, targetDate);

        // 잔여량 데이터 조회
        List<CaffeineResidual> residuals = residualService.getCaffeineResidualsByTimeRange(user,
            currentDateTime);

        int intakeRate = calculateIntakeRate(dailyStatistics.getTotalCaffeineMg(),
            user.getCaffeinInfo().getDailyCaffeineLimitMg()
        );

        List<DailyCaffeineReportResponse.HourlyCaffeineInfo> hourlyInfo = createHourlyCaffeineInfo(residuals, currentDateTime);

        return DailyReportMapper.toResponse(
            user, dailyStatistics, intakeRate, hourlyInfo
        );

//        return new DailyCaffeineReportResponse(
//            user.getNickname(),
//            user.getCaffeinInfo().getDailyCaffeineLimitMg(),
//            dailyStatistics.getTotalCaffeineMg(),
//            calculateIntakeRate(
//                    dailyStatistics.getTotalCaffeineMg(),
//                    user.getCaffeinInfo().getDailyCaffeineLimitMg()
//            ),
//            dailyStatistics.getAiMessage(),
//            100F,
//            createHourlyCaffeineInfo(residuals, currentDateTime)
//        );
    }

    private int calculateIntakeRate(float dailyTotal, float userDailyLimit) {
        return (int) ((dailyTotal / userDailyLimit) * 100);
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
        // 시간 비교를 단순화: 년, 월, 일, 시간 비교
        if (residualTime.isEqual(timePoint)) {
            totalCaffeine += residual.getResidueAmountMg();
            count++;
        }
        return count > 0 ? totalCaffeine / count : 0f;
    }

    private LocalDateTime getReportBaseDateTime(LocalDate targetDate, LocalTime targetTime) {
        ZoneId krTimeZone = ZoneId.of("Asia/Seoul"); // 또는 클래스 레벨 상수로 선언

        if (targetDate != null && targetTime != null) {
            // 시스템 기본 타임존으로 ZonedDateTime 생성 후 한국 시간으로 변환
            ZonedDateTime zonedDateTime = ZonedDateTime.of(targetDate, targetTime, ZoneId.systemDefault())
                .withZoneSameInstant(krTimeZone);
            return zonedDateTime.toLocalDateTime();
        } else {
            return LocalDateTime.now(krTimeZone);
        }
    }
}