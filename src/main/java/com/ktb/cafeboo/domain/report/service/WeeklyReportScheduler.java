package com.ktb.cafeboo.domain.report.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.dto.CoffeeTimeStats;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import com.ktb.cafeboo.domain.report.repository.WeeklyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyReportRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyReportResponse;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WeeklyReportScheduler {
    private final UserRepository userRepository;
    private final AiServerClient aiServerClient;
    private final WeeklyReportRepository weeklyReportRepository;
    private final CaffeineIntakeRepository intakeRepository;
    private final CaffeineResidualService caffeineResidualService;

    @Scheduled(cron = "0 0 0 * * SUN") // 매주 일요일 0시
    public void generateWeeklyReports() {
        LocalDate endDate = LocalDate.now().minusDays(1); // 지난 주 토요일
        LocalDate startDate = endDate.minusDays(6);       // 지난 주 일요일

//        int targetYear = 2024;
//        int targetWeekNum = 19;
//
//        // ISO 8601 표준 (월요일 시작, 한 주의 최소 4일 포함)을 따르는 WeekFields 객체 생성
//        WeekFields isoWeekFields = WeekFields.of(Locale.getDefault());
//
//        // 해당 년도의 첫 번째 날짜
//        LocalDate firstDayOfYear = LocalDate.of(targetYear, 1, 1);
//
//        // 첫 번째 주(week 1)의 첫 번째 날 (월요일) 찾기
//        LocalDate firstMonday = firstDayOfYear;
//        if (firstMonday.getDayOfWeek() != DayOfWeek.MONDAY) {
//            firstMonday = firstMonday.with(WeekFields.ISO.dayOfWeek(), 1); // 해당 주의 월요일로 이동
//            if (firstMonday.getYear() > targetYear) {
//                firstMonday = firstDayOfYear.plusWeeks(1).with(WeekFields.ISO.dayOfWeek(), 1);
//            }
//        }
//
//        // targetWeekNum 주의 시작 날짜 계산
//        LocalDate startDate = firstMonday.plusWeeks(targetWeekNum - 1);
//
//        // targetWeekNum 주의 마지막 날짜 계산 (일요일)
//        LocalDate endDate = startDate.plusDays(6);

        int weeknum = startDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = startDate.getYear();

        List<User> users = userRepository.findAll();
        List<CreateWeeklyReportRequest> requests = users.stream()
            .map(user -> createWeeklyReportRequest(user, startDate, endDate))
            .collect(Collectors.toList());

        if (!requests.isEmpty()) {
            CreateWeeklyReportResponse response = aiServerClient.createWeeklyReportAnalysis(requests);

            if (response.getStatus().equals(HttpStatus.OK.toString())) {
                log.info("AI 서버에 주간 보고서 생성 요청 성공. 콜백을 기다립니다.");
            } else {
                log.error("AI 서버 주간 보고서 생성 요청 실패: {}", response.getStatus());
            }
        } else {
            log.info("생성할 주간 보고서 요청 데이터가 없습니다.");
        }
    }



    private CreateWeeklyReportRequest createWeeklyReportRequest(User user, LocalDate startDate, LocalDate endDate) {
        List<CaffeineIntake> intakes = intakeRepository.findByUserIdAndIntakeTimeBetween(user.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<DayOfWeek, Double> dailyCaffeine = intakes.stream()
            .collect(Collectors.groupingBy(
                record -> record.getIntakeTime().getDayOfWeek(),
                Collectors.summingDouble(CaffeineIntake::getCaffeineAmountMg)
            ));
        DayOfWeek highlightDayHigh = dailyCaffeine.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        DayOfWeek highlightDayLow = dailyCaffeine.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        String highlightDayHighStr = (highlightDayHigh != null) ? highlightDayHigh.toString().substring(0, 3) : "Mon";
        String highlightDayLowStr = (highlightDayLow != null) ? highlightDayLow.toString().substring(0, 3) : "Mon";

        CoffeeTimeStats coffeeTimeStats = calculate(user, intakes);
        LocalTime firstAvg = coffeeTimeStats.firstAvg;
        LocalTime lastAvg = coffeeTimeStats.lastAvg;
        int lateNightDays = coffeeTimeStats.lateNightDays;

        double totalCaffeine = intakes.stream()
            .mapToDouble(CaffeineIntake::getCaffeineAmountMg)
            .sum();
        double dailyAvg = totalCaffeine / 7.0;
        double recommendedLimit = user.getCaffeinInfo().getDailyCaffeineLimitMg();

        String period = startDate.toString() + " ~ " + endDate.toString();

        int over100mgBeforeSleepDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            CaffeineResidual residual = caffeineResidualService.findByUserAndTargetDateAndHour(user, date.atStartOfDay(), user.getHealthInfo().getSleepTime().getHour());
            if (residual != null && residual.getResidueAmountMg() > 100.0) {
                over100mgBeforeSleepDays++;
            }
        }

        return CreateWeeklyReportRequest.builder()
            .userId(user.getId().toString())
            .data(CreateWeeklyReportRequest.Data.builder() // Data 객체 빌더 시작
                .period(period)
                .avgCaffeinePerDay((int) dailyAvg)
                .recommendedDailyLimit((int) recommendedLimit)
                .percentageOfLimit((int) (dailyAvg * 100 / recommendedLimit))
                .highlightDayHigh(highlightDayHighStr)
                .highlightDayLow(highlightDayLowStr)
                .firstCoffeeAvg(firstAvg != null ? firstAvg.toString() : null)
                .lastCoffeeAvg(lastAvg != null ? lastAvg.toString() : null)
                .lateNightCaffeineDays(lateNightDays)
                .over100mgBeforeSleepDays(over100mgBeforeSleepDays)
                .averageSleepQuality("good") // averageSleepQuality 설정 (실제 값으로 대체)
                .build()) // Data 객체 빌더 종료
            .callbackUrl("/") // callbackUrl 설정 (필요한 경우)
            .build();

//        String period = startDate.toString() + " ~ " + endDate.toString();
//
//        // highlight_day_high 계산
//        Map<DayOfWeek, Double> dailyCaffeine = intakes.stream()
//            .collect(Collectors.groupingBy(record -> record.getIntakeTime().getDayOfWeek(),
//                Collectors.summingDouble(CaffeineIntake::getCaffeineAmountMg)));
//        DayOfWeek highlightDayHigh = dailyCaffeine.entrySet().stream()
//            .max(Map.Entry.comparingByValue())
//            .map(Map.Entry::getKey)
//            .orElse(null);
//
//        // hightlight_day_low 계산
//        DayOfWeek highlightDayLow = dailyCaffeine.entrySet().stream()
//            .min(Map.Entry.comparingByValue())
//            .map(Map.Entry::getKey)
//            .orElse(null);
//        String highlightDayHighStr = (highlightDayHigh != null) ? highlightDayHigh.toString().substring(0, 3) : null;
//        String highlightDayLowStr = (highlightDayLow != null) ? highlightDayLow.toString().substring(0, 3) : null;
//
//        CoffeeTimeStats coffee_time_stats = calculate(intakes);
//        LocalTime firstAvg = coffee_time_stats.firstAvg;
//        LocalTime lastAvg = coffee_time_stats.lastAvg;
//        int lateNightDays = coffee_time_stats.lateNightDays;
//
//        CreateWeeklyReportRequest request = CreateWeeklyReportRequest.builder()
//            .userId(userId.toString())
//            .period(period)
//            .avgCaffeinePerDay((int)dailyAvg)
//            .recommendedDailyLimit((int)userCaffeinInfo.getDailyCaffeineLimitMg())
//            .percentageOfLimit((int)dailyAvg / (int)userCaffeinInfo.getDailyCaffeineLimitMg())
//            .highlightDayHigh(highlightDayHighStr)
//            .highlightDayLow(highlightDayLowStr)
//            .firstCoffeeAvg(firstAvg.toString())
//            .lastCoffeeAvg(lastAvg.toString())
//            .lateNightCaffeineDays(lateNightDays)
//            .over100mgBeforeSleepDays(0)
//            .build();
//
//        System.out.println("userId: " + request.getUserId());
//        System.out.println("period: " + request.getPeriod());
//        System.out.println("avgCaffeinePerDay: " + request.getAvgCaffeinePerDay());
//        System.out.println("recommendedDailyLimit: " + request.getRecommendedDailyLimit());
//        System.out.println("percentageOfLimit: " + request.getPercentageOfLimit());
//        System.out.println("highlightDayHigh: " + request.getHighlightDayHigh());
//        System.out.println("highlightDayLow: " + request.getHighlightDayLow());
//        System.out.println("firstCoffeeAvg: " + request.getFirstCoffeeAvg());
//        System.out.println("lastCoffeeAvg: " + request.getLastCoffeeAvg());
//        System.out.println("lateNightCaffeineDays: " + request.getLateNightCaffeineDays());
//        System.out.println("over100mgBeforeSleepDays: " + request.getOver100mgBeforeSleepDays());
//
//        CreateWeeklyReportResponse response = aiServerClient.createWeeklyReportAnalysis(request);
//
//        String summaryMessage = "이번 주 평균 섭취량은 권장량의 " + (dailyAvg * 100 / 400) + "% 수준입니다."
    }

    private static CoffeeTimeStats calculate(User user, List<CaffeineIntake> intakes) {
        // 1. 날짜별로 그룹핑
        Map<LocalDate, List<CaffeineIntake>> byDate = intakes.stream()
            .collect(Collectors.groupingBy(i -> i.getIntakeTime().toLocalDate()));

        List<LocalTime> firstTimes = new ArrayList<>();
        List<LocalTime> lastTimes = new ArrayList<>();
        int lateNightDays = 0;

        LocalTime wakeUpTime = user.getHealthInfo().getWakeUpTime();

        for (Map.Entry<LocalDate, List<CaffeineIntake>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<CaffeineIntake> dayIntakes = entry.getValue();

            // intakeTime 기준 정렬
            List<LocalTime> times = dayIntakes.stream()
                .map(i -> i.getIntakeTime().toLocalTime())
                .sorted()
                .collect(Collectors.toList());

            // 첫/마지막 커피 시간
            firstTimes.add(times.get(0));
            lastTimes.add(times.get(times.size() - 1));

            // 22시 ~ 다음날 기상시간 사이 섭취 기록이 있는지 체크
            LocalDateTime lateStart = date.atTime(22, 0);
            LocalDateTime lateEnd = date.plusDays(1).atTime(wakeUpTime);

            boolean hasLateNight = dayIntakes.stream()
                .map(CaffeineIntake::getIntakeTime)
                .anyMatch(dt -> !dt.isBefore(lateStart) && dt.isBefore(lateEnd));

            if (hasLateNight)
                lateNightDays++;
        }

        // 평균 시간 계산 (초 단위로 변환 후 평균)
        LocalTime firstAvg = averageLocalTimes(firstTimes);
        LocalTime lastAvg = averageLocalTimes(lastTimes);
        return new CoffeeTimeStats(firstAvg, lastAvg, lateNightDays);
    }

    private static LocalTime averageLocalTimes(List<LocalTime> times) {
        if (times.isEmpty()) return null;
        double avgSeconds = times.stream()
            .mapToLong(t -> t.toSecondOfDay())
            .average()
            .orElse(0);

        int avgMinutes = (int) Math.round(avgSeconds / 60.0); // 반올림해서 분 단위로
        int hour = avgMinutes / 60;
        int minute = avgMinutes % 60;
        return LocalTime.of(hour, minute);
    }
}
