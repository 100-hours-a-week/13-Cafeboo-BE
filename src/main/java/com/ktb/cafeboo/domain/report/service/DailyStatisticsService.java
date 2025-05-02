package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.repository.DailyStatisticsRepository;
import com.ktb.cafeboo.domain.user.model.User;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DailyStatisticsService {

    private final DailyStatisticsRepository dailyStatisticsRepository;
    private final CaffeineResidualRepository caffeineResidualRepository;

    private final WeeklyReportService weeklyReportService;

    private static final int HOURS_RANGE = 17; // 기준 시간 전 후 17시간

    /**
     * ISO 8601 기준으로 주차 ID를 생성합니다.
     * 형식: YYYYWW (예: 202411 - 2024년 11주차)
     */
    private Long calculateWeeklyStatisticsId(LocalDate date) {
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // YYYYWW 형식으로 ID 생성 (예: 202411)
        return Long.valueOf(String.format("%d%02d", year, weekOfYear));
    }

    /**
     * 일일 통계 데이터를 갱신합니다. 섭취 내역이 추가됨에 따라 일일 섭취 카페인 수치를 갱신합니다.
     * @param user 일일 통계를 기록할 유저 정보
     * @param date 섭취 내역을 등록/수정한 시점의 연-월-일 정보
     * @param additionalCaffeine 섭취 내역이 추가/변경됨에 따라 변경될 카페인 수치
     */
    public void updateDailyStatistics(User user, LocalDate date, float additionalCaffeine) {
        DailyStatistics statistics = dailyStatisticsRepository
            .findByUserIdAndDate(user.getId(), date)
            .orElseGet(() -> createDailyStatistics(user, date));

        statistics.setTotalCaffeineMg(statistics.getTotalCaffeineMg() + additionalCaffeine);
        DailyStatistics savedStatistics = dailyStatisticsRepository.save(statistics);

//        WeeklyReport weeklyReport = weeklyReportService.getOrCreateWeeklyReport(user, date);
//        weeklyReportService.updateWeeklyReport(weeklyReport, getUserDailyLimit(userId));
    }

    /**
     * 일일 통계 데이터를 생성합니다.
     * @param user 일일 통계를 기록할 유저 정보
     * @param date 섭취 내역을 등록한 시점의 연-월-일 정보
     */
    private DailyStatistics createDailyStatistics(User user, LocalDate date) {
        //해당 날짜가 속한 주의 정보를 가져옴. 없는 경우 새롭게 생성 후 WeeklyReport 테이블에 저장.
        WeeklyReport weeklyReport = weeklyReportService.getOrCreateWeeklyReport(user, date);

        //일일 통계 데이터를 반환
        return DailyStatistics.builder()
            .user(user)
            .date(date)
            .totalCaffeineMg(0f)
            .weeklyStatisticsId(weeklyReport.getId())
            .build();
    }

    /**
     * 특정 날짜의 총 카페인 섭취량을 조회합니다.
     *
     * @param date 조회할 날짜
     * @return 총 카페인 섭취량 (mg)
     */
    public float getTotalCaffeineForDate(LocalDate date) {
        return dailyStatisticsRepository.findTotalCaffeineByDate(date)
            .orElse(0f);
    }

    /**
     * 현재 시간 기준 전후 35시간의 카페인 잔존량 데이터를 조회합니다.
     *
     * @param user 조회할 사용자
     * @param currentDateTime 현재 시간
     * @return 35시간 범위의 카페인 잔존량 데이터
     */
    public List<CaffeineResidual> getCaffeineResidualsByTimeRange(
        User user,
        LocalDateTime currentDateTime) {

        LocalDate currentDate = currentDateTime.toLocalDate();
        LocalDate previousDate = currentDate.minusDays(1);
        LocalDate nextDate = currentDate.plusDays(1);

        int currentHour = currentDateTime.getHour();

        // 이전 날짜의 시작 시간 계산
        int previousStartHour = (currentHour + 24 - HOURS_RANGE) % 24;
        // 다음 날짜의 종료 시간 계산
        int nextEndHour = (currentHour + HOURS_RANGE) % 24;

        return caffeineResidualRepository.findResidualsByTimeRange(
            user,
            previousDate,
            currentDate,
            nextDate,
            previousStartHour,
            nextEndHour
        );
    }

    /**
     * 조회된 데이터를 시간별로 변환합니다.
     */
    public List<HourlyResidualData> convertToHourlyData(
        List<CaffeineResidual> residuals,
        LocalDateTime currentDateTime) {

        List<HourlyResidualData> result = new ArrayList<>();
        LocalDateTime startTime = currentDateTime.minusHours(HOURS_RANGE);

        // 총 35시간(이전 17시간 + 현재 1시간 + 이후 17시간)의 데이터 생성
        for (int i = 0; i < (HOURS_RANGE * 2 + 1); i++) {
            LocalDateTime targetDateTime = startTime.plusHours(i);
            LocalDate targetDate = targetDateTime.toLocalDate();
            int targetHour = targetDateTime.getHour();

            // 해당 시간의 잔존량 찾기
            float residualAmount = residuals.stream()
                .filter(r -> r.getTargetDate().equals(targetDate) && r.getHour() == targetHour)
                .findFirst()
                .map(CaffeineResidual::getResidueAmountMg)
                .orElse(0f);

            result.add(new HourlyResidualData(targetDateTime, residualAmount));
        }

        return result;
    }

    @Getter
    @AllArgsConstructor
    public static class HourlyResidualData {
        private LocalDateTime dateTime;
        private float residualAmount;

        public String getFormattedDateTime() {
            return dateTime.format(DateTimeFormatter.ofPattern("HH"));
        }
    }
}
