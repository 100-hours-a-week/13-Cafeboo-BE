package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
public class PredictCanIntakeCaffeineRequest {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("current_time")
    private double currentTime;
    @JsonProperty("sleep_time")
    private double sleepTime;
    @JsonProperty("caffeine_limit")
    private int caffeineLimit;
    @JsonProperty("current_caffeine")
    private int currentCaffeine;
    @JsonProperty("target_residual_at_sleep")
    private double targetResidualAtSleep;
    @JsonProperty("residual_at_sleep")
    private double residualAtSleep;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("age")
    private int age;
    @JsonProperty("weight")
    private float weight;
    @JsonProperty("height")
    private float height;
    @JsonProperty("is_smoker")
    private int isSmoker;
    @JsonProperty("take_hormonal_contraceptive")
    private int takeHormonalContraceptive;
    @JsonProperty("caffeine_sensitivity")
    private int caffeineSensitivity;
}
