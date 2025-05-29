package com.ktb.cafeboo.domain.ai.service;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class CaffeineRecommendationService {

    private final AiServerClient aiServerClient;

    public float getPredictedCaffeineLimitByRule(User user, int caffeineSensitivity) {
        log.info("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] 호출 시작 - caffeineSensitivity={}", caffeineSensitivity);

        // [RULE 기반] 사용자 상태정보를 바탕으로 하루 최대 카페인 허용량 예측
        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            log.warn("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] 사용자 건강 정보 없음 - userId={}", user.getId());
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        PredictCaffeineLimitByRuleRequest request = PredictCaffeineLimitByRuleRequest.builder()
                .userId(user.getId().toString())
                .gender(healthInfo.getGender())
                .age(healthInfo.getAge())
                .weight(healthInfo.getWeight())
                .height(healthInfo.getHeight())
                .isSmoker(healthInfo.getSmoking() ? 1 : 0)
                .takeHormonalContraceptive(healthInfo.getTakingBirthPill() ? 1 : 0)
                .caffeineSensitivity(caffeineSensitivity)
                .build();

        PredictCaffeineLimitByRuleResponse response = aiServerClient.predictCaffeineLimitByRule(request);

        if (!"success".equals(response.getStatus())) {
            log.error("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] AI 서버 예측 실패 - code={}, detail={}",
                    response.getData().getCode(),
                    response.getData().getDetail());
            throw new CustomApiException(ErrorStatus.AI_SERVER_ERROR);
        }

        float result = response.getData().getMaxCaffeineMg();
        log.info("[CaffeineRecommendationService.getPredictedCaffeineLimitByRule] 예측 성공 - maxCaffeineMg={}", result);

        return result;
    }
}