package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PredictCaffeineLimitRequest {
    private String userId;
    private String modelHint;
    private String gender;
    private int age;
    private float weight;
    private int height;
    private int isSmoker;
    private int takeHormonalContraceptive;
    private int caffeineSensitivity;
}
