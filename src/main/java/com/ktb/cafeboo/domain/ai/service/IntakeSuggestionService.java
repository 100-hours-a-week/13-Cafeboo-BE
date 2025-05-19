package com.ktb.cafeboo.domain.ai.service;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.infra.ai.client.AiServerClient;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineResponse;
import java.time.LocalTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntakeSuggestionService {
    private final AiServerClient aiServerClient;

    public String getPredictedIntakeSuggestion(User user, int currentCaffeine, double residualAtSleep){
        PredictCanIntakeCaffeineRequest request = PredictCanIntakeCaffeineRequest.builder()
            .userId(user.getId().toString())
            .currentTime(convertTimeToFloat(LocalTime.now()))
            .sleepTime(convertTimeToFloat(user.getHealthInfo().getSleepTime()))
            .caffeineLimit(Math.round(user.getCaffeinInfo().getDailyCaffeineLimitMg()))
            .currentCaffeine(currentCaffeine)
            .caffeineSensitivity(user.getCaffeinInfo().getCaffeineSensitivity())
            .targetResidualAtSleep(50f)
            .residualAtSleep(residualAtSleep)
            .gender(user.getHealthInfo().getGender())
            .age(user.getHealthInfo().getAge())
            .weight(user.getHealthInfo().getWeight())
            .height(user.getHealthInfo().getHeight())
            .isSmoker(user.getHealthInfo().getSmoking() ? 1 : 0)
            .takeHormonalContraceptive(user.getHealthInfo().getTakingBirthPill() ? 1 : 0)
            .build();

        PredictCanIntakeCaffeineResponse response = aiServerClient.predictCanIntakeCaffeine(request);

        String message = "";

        if(Objects.equals(response.getStatus(), "success")){
            if(Objects.equals(response.getData().getCaffeineStatus(), "N")){
                message += " 카페인을 추가로 섭취하면 수면에 영향을 줄 수 있어요.";
            }
            else if (Objects.equals(response.getData().getCaffeineStatus(), "Y")){
                message += " 카페인을 추가로 섭취해도 수면에 영향이 없어요.";
            }
        }
        return message;
    }

    private static float convertTimeToFloat(LocalTime time) {
        if (time == null) {
            return 0.0f; // 또는 다른 적절한 기본값
        }
        return time.getHour() + (float) time.getMinute() / 60.0f;
    }
}
