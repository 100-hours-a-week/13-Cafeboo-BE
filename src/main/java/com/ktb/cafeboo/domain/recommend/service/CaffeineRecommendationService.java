package com.ktb.cafeboo.domain.recommend.service;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleResponse;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.domain.user.repository.UserCaffeineInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaffeineRecommendationService {

    private final AiServerClient aiServerClient;
    private final UserRepository userRepository;
    private final UserCaffeineInfoRepository userCaffeineInfoRepository;

    public float getPredictedCaffeineLimitByRule(User user, int caffeineSensitivity) {
        // [RULE 기반] 사용자 상태정보를 바탕으로 하루 최대 카페인 허용량 예측
        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        PredictCaffeineLimitByRuleRequest request = PredictCaffeineLimitByRuleRequest.builder()
                .userId(user.getId().toString())
                .modelHint("rule")  // 명시적으로 rule 기반임을 전달
                .gender(healthInfo.getGender())
                .age(healthInfo.getAge())
                .weight(healthInfo.getWeight())
                .height((int) healthInfo.getHeight())
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

        return response.getData().getMaxCaffeineMg();
    }
}
