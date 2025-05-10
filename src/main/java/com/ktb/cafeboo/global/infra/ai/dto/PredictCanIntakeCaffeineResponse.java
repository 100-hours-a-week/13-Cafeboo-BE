package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PredictCanIntakeCaffeineResponse {
    private String status;
    private String message;
    private Data data;

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Data {
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("caffeine_status")
        private String caffeineStatus;
    }
}
