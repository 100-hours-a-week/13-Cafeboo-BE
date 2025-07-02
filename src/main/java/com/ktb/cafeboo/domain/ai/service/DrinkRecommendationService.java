package com.ktb.cafeboo.domain.ai.service;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeineInfo;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrinkRecommendationService {
    private final AiServerClient aiServerClient;
    private final UserService userService;

    public CreateDrinkRecommendationResponse getRecommendationResult(Long userId){

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

        CreateDrinkRecommendationRequest request = CreateDrinkRecommendationRequest.builder()
            .gender(healthInfo.getGender())
            .age(healthInfo.getAge())
            .weight(healthInfo.getWeight())
            .height(healthInfo.getHeight())
            .isSmoking(healthInfo.getSmoking() ? 1 : 0)
            .isTakingBirthPill(healthInfo.getTakingBirthPill() ? 1 : 0)
            .isPregnant(healthInfo.getPregnant() ? 1 : 0)
            .caffeineSensitivity(caffeineInfo.getCaffeineSensitivity())
            .build();

        CreateDrinkRecommendationResponse response = aiServerClient.createCoffeeRecommendation(request);

        if (!"success".equals(response.getStatus())) {
            log.error("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] AI 서버 예측 실패 - message={}", response.getMessage());
            throw new CustomApiException(ErrorStatus.AI_SERVER_ERROR);
        }

        return response;
    }
}
