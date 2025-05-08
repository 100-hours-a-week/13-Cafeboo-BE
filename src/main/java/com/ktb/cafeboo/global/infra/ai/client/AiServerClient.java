package com.ktb.cafeboo.global.infra.ai.client;

import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitResponse;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final WebClient aiServerWebClient;

    public PredictCaffeineLimitResponse predictCaffeineLimit(PredictCaffeineLimitRequest request) {
        return aiServerWebClient.post()
                .uri("/internal/ai/predict_limit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictCaffeineLimitResponse.class)
                .block();
    }

    public PredictCanIntakeCaffeineResponse predictCanIntakeCaffeine(PredictCanIntakeCaffeineRequest request) {
        return aiServerWebClient.post()
                .uri("/internal/ai/can_intake_caffeine")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictCanIntakeCaffeineResponse.class)
                .block();
    }
}

