package com.ktb.cafeboo.global.infra.ai.client;

import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateDrinkRecommendationResponse;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisRequest;
import com.ktb.cafeboo.global.infra.ai.dto.CreateWeeklyAnalysisResponse;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCaffeineLimitByRuleResponse;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineRequest;
import com.ktb.cafeboo.global.infra.ai.dto.PredictCanIntakeCaffeineResponse;
import com.ktb.cafeboo.global.infra.ai.dto.ToxicityDetectionRequest;
import com.ktb.cafeboo.global.infra.ai.dto.ToxicityDetectionResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final WebClient aiServerWebClient;

    public PredictCaffeineLimitByRuleResponse predictCaffeineLimitByRule(
        PredictCaffeineLimitByRuleRequest request) {
        return aiServerWebClient.post()
                .uri("/internal/ai/predict_limit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictCaffeineLimitByRuleResponse.class)
                .block();
    }

    public PredictCanIntakeCaffeineResponse predictCanIntakeCaffeine(PredictCanIntakeCaffeineRequest request) {
        return aiServerWebClient.post()
                .uri("/internal/ai/can_intake_caffeine")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictCanIntakeCaffeineResponse.class)
                .block();
    }

    public CreateWeeklyAnalysisResponse createWeeklyReportAnalysis(CreateWeeklyAnalysisRequest requests){
        return aiServerWebClient.post()
            .uri("/internal/ai/caffeine_weekly_reports")
            .bodyValue(requests)
            .retrieve()
            .bodyToMono(CreateWeeklyAnalysisResponse.class)
            .block();
    }

    public ToxicityDetectionResponse detectToxicity(ToxicityDetectionRequest request) {
        return aiServerWebClient.post()
                .uri("/internal/ai/toxicity_detect")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ToxicityDetectionResponse.class)
                .block();
    }

    public CreateDrinkRecommendationResponse createCoffeeRecommendation(
        CreateDrinkRecommendationRequest request){
        return aiServerWebClient.post()
            .uri("/internal/ai/drink_recommendation")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(CreateDrinkRecommendationResponse.class)
            .block();
    }
}

