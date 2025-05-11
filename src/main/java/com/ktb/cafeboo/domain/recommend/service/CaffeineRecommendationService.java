package com.ktb.cafeboo.domain.recommend.service;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Slf4j
@Service
@RequiredArgsConstructor
public class CaffeineRecommendationService {

    private final AiServerClient aiServerClient;

    public float getPredictedCaffeineLimitByRule(User user, int caffeineSensitivity) {
        // [RULE 기반] 사용자 상태정보를 바탕으로 하루 최대 카페인 허용량 예측
        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
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
            log.error("[AI 서버 호출 오류] code: {}, detail: {}",
                    response.getData().getCode(),
                    response.getData().getDetail()
            );
            throw new CustomApiException(ErrorStatus.AI_SERVER_ERROR);
        }

        float result = response.getData().getMaxCaffeineMg();
        log.info("[AI 서버 호출 성공] 최대 허용 카페인량을 성공적으로 예측하였습니다.");
        log.info("예측 최대 허용 카페인량: {}", result);

        return result;
    }
}
