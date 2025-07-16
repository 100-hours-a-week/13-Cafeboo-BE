package com.ktb.cafeboo.domain.ai.service;

import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse;
import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.report.service.WeeklyReportService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeineInfo;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationResponse;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrinkRecommendationService {
    private final AiServerClient aiServerClient;
    private final UserService userService;
    private final DailyStatisticsService dailyStatisticsService;
    private final WeeklyReportService weeklyReportService;

    public CreateDrinkRecommendationResponse getRecommendationResult(Long userId){
        log.info("[DrinkRecommendationService.getRecommendationResult] - userId : {}", userId);
        User user = userService.findUserById(userId);

        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            log.warn("[CreateDrinkRecommendationResponse.getRecommendationResult] 사용자 건강 정보 없음 - userId={}", user.getId());
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        UserCaffeineInfo caffeineInfo = user.getCaffeinInfo();
        if (caffeineInfo == null) {
            log.warn("[CaffeineRecommendationService.getRecommendationResult] 사용자 카페인 정보 없음 - userId={}", user.getId());
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_NOT_FOUND);
        }

        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        String targetYear = String.valueOf(now.getYear());
        String targetMonth = String.valueOf(now.getMonthValue());
        String targetWeek = String.valueOf(now.get(weekFields.weekOfWeekBasedYear()));

        List<DailyStatistics> dailyStats = dailyStatisticsService.getDailyStatisticsForWeek(userId, targetYear, targetMonth, targetWeek);

        WeeklyCaffeineReportResponse weeklyCaffeineReportResponse = weeklyReportService.getWeeklyReport(userId, targetYear, targetMonth, targetWeek, dailyStats);

        CreateDrinkRecommendationRequest request = CreateDrinkRecommendationRequest.builder()
            .gender(healthInfo.getGender())
            .age(healthInfo.getAge())
            .height(healthInfo.getHeight())
            .weight(healthInfo.getWeight())
            .isSmoking(healthInfo.getSmoking() ? 1 : 0)
            .isTakingBirthPill(healthInfo.getTakingBirthPill() ? 1 : 0)
            .isPregnant(healthInfo.getPregnant() ? 1 : 0)
            .caffeineSensitivity(caffeineInfo.getCaffeineSensitivity())
            .avgIntakePerDay(caffeineInfo.getAverageDailyCaffeineIntake())
            .avgCaffeineAmount(weeklyCaffeineReportResponse.dailyCaffeineAvg())
            .dailyCaffeineLimit(caffeineInfo.getDailyCaffeineLimitMg())
            .build();

        log.info("[DrinkRecommendationService.getRecommendationResult] - AI 추천 결과 생성 요청");
        CreateDrinkRecommendationResponse response = aiServerClient.createCoffeeRecommendation(request);
        log.info("AI 서버 응답 DTO: {}", response);
        log.info("[DrinkRecommendationService.getRecommendationResult] - AI 추천 결과 생성 성공");

        if (!"success".equals(response.getStatus())) {
            log.error("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] AI 서버 예측 실패 - message={}", response.getMessage());
            throw new CustomApiException(ErrorStatus.AI_SERVER_ERROR);
        }

        return response;
    }
}
