package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PredictCaffeineLimitResponse {
    private String status;
    private String message;
    private Data data;

    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Data {
        private String userId;
        private float maxCaffeineMg;
    }
}
