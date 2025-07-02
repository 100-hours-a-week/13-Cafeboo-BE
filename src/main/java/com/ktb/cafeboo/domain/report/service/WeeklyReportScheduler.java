package com.ktb.cafeboo.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineResidualService;
import com.ktb.cafeboo.domain.report.dto.CoffeeTimeStats;
import com.ktb.cafeboo.domain.report.repository.WeeklyReportRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Scheduled(cron = "0 0 12 ? * MON") // 매주 월요일 오전 9시, 현재는 12시로 설정
    public CreateWeeklyAnalysisResponse generateWeeklyReports() {
        log.info("[IntakeSuggestionService.getPredictedIntakeSuggestion] 호출 시작");

        List<User> users = userRepository.findAll();
        String callbackUrl = "http://localhost:8080/api/v1/reports/weekly/ai_callback";

        CreateWeeklyAnalysisRequest batchRequest = createWeeklyAnalysisRequest(users, callbackUrl);

        CreateWeeklyAnalysisResponse response = aiServerClient.createWeeklyReportAnalysis(batchRequest);

        if (!"success".equals(response.getStatus())) {
            log.error("[CaffeineRecommendationService.generateWeeklyReports] - AI 주간 섭취 내역 평가 리포트 생성 요청 실패");
            throw new CustomApiException(ErrorStatus.AI_SERVER_ERROR);
        }

        return response;
    }



    private CreateWeeklyAnalysisRequest createWeeklyAnalysisRequest(List<User> users, String callbackUrl) {
        log.info("[IntakeSuggestionService.createWeeklyAnalysisRequest] 호출 시작");
        List<CreateWeeklyAnalysisRequest.UserReportData> userReportDataList = users.stream()
            .map(user -> {
                LocalDate endDate = LocalDate.now().minusDays(1); // 예시: 스케줄링 기준에 따라 변경
                LocalDate startDate = endDate.minusDays(6);

                List<CaffeineIntake> intakes = intakeRepository.findByUserIdAndIntakeTimeBetween(user.getId(), startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

                if (intakes.isEmpty()) {
                    return null;
                }

                if(user.getHealthInfo() == null){
                    return null;
                }

                UserHealthInfo userHealthInfo = user.getHealthInfo();
                if (userHealthInfo.getGender() == null ||
                    userHealthInfo.getSmoking() == null ||
                    userHealthInfo.getTakingBirthPill() == null ||
                    userHealthInfo.getHasLiverDisease() == null ||
                    userHealthInfo.getPregnant() == null) {
                    return null; // healthInfo 내부 필드 중 하나라도 null이면 null 반환
                }

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
                LocalTime firstAvg = coffeeTimeStats.firstAvg();
                LocalTime lastAvg = coffeeTimeStats.lastAvg();
                int lateNightDays = coffeeTimeStats.lateNightDays();

                double totalCaffeine = intakes.stream()
                    .mapToDouble(CaffeineIntake::getCaffeineAmountMg)
                    .sum();
                double dailyAvg = totalCaffeine / 7.0;
                double recommendedLimit = user.getCaffeinInfo() != null ? user.getCaffeinInfo().getDailyCaffeineLimitMg()
                                                                        : 400;

                String period = startDate.toString() + " ~ " + endDate.toString();
                LocalTime userSleepTime = (user.getHealthInfo() != null && user.getHealthInfo().getSleepTime() != null) ? user.getHealthInfo().getSleepTime()
                                                                                                                        : LocalTime.of(0, 0);

                int over100mgBeforeSleepDays = 0;
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    CaffeineResidual residual = caffeineResidualService.findByUserAndTargetDateAndHour(user, date.atStartOfDay(), userSleepTime.getHour());
                    if (residual != null && residual.getResidueAmountMg() > 100.0) {
                        over100mgBeforeSleepDays++;
                    }
                }

                CreateWeeklyAnalysisRequest.Data userData = CreateWeeklyAnalysisRequest.Data.builder()
                    .gender(userHealthInfo.getGender())
                    .age(userHealthInfo.getAge())
                    .weight(userHealthInfo.getWeight())
                    .height(userHealthInfo.getHeight())
                    .isSmoker(userHealthInfo.getSmoking() ? 1 : 0)
                    .takeHormonalContraceptive(userHealthInfo.getTakingBirthPill() ? 1 : 0)
                    .hasLiverDisease(userHealthInfo.getHasLiverDisease() ? 1 : 0)
                    .isPregnant(userHealthInfo.getPregnant() ? 1 : 0)
                    .nickname(user.getNickname())
                    .period(period)
                    .avgCaffeinePerDay((float) dailyAvg)
                    .recommendedDailyLimit((float)recommendedLimit)
                    .percentageOfLimit((float)(dailyAvg * 100 / recommendedLimit))
                    .highlightDayHigh(highlightDayHighStr)
                    .highlightDayLow(highlightDayLowStr)
                    .firstCoffeeAvg(firstAvg != null ? firstAvg.toString() : "")
                    .lastCoffeeAvg(lastAvg != null ? lastAvg.toString() : "")
                    .lateNightCaffeineDays(lateNightDays)
                    .over100mgBeforeSleepDays(over100mgBeforeSleepDays)
                    .build();

                return CreateWeeklyAnalysisRequest.UserReportData.builder()
                    .userId(user.getId().toString())
                    .data(userData)
                    .build();
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());

        log.info("[IntakeSuggestionService.createWeeklyAnalysisRequest] WeeklyAnalysisRequest 생성 완료");
        return CreateWeeklyAnalysisRequest.builder()
            .callbackUrl(callbackUrl)
            .users(userReportDataList)
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

        LocalTime userWakeupTime = (user.getHealthInfo() != null && user.getHealthInfo().getWakeUpTime() != null) ? user.getHealthInfo().getWakeUpTime()
            : LocalTime.of(7, 0);

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
            LocalDateTime lateEnd = date.plusDays(1).atTime(userWakeupTime);

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
