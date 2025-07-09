package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateDrinkRecommendationResponse {

    private String status;
    private String message;
    private InnerAiResponseData data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class InnerAiResponseData {
        private String status;
        private RecommendationDetails data;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RecommendationDetails {
        private List<Recommendation> drinkIds;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Recommendation {
        @JsonProperty("drink_id")
        private Long drinkId;
        private Double score;
    }
}