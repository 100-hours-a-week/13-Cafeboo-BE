package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PredictCanIntakeCaffeineRequest {
    private String userId;
    private double currentTime;
    private double sleepTime;
    private int caffeineLimit;
    private int currentCaffeine;
    private int caffeineLeft;
    private int plannedCaffeineIntake;
    private double targetResidualAtSleep;
    private double residualAtSleep;
    private String modelHint;
    private String gender;
    private int age;
    private float weight;
    private int height;
    private int isSmoker;
    private int takeHormonalContraceptive;
    private int caffeineSensitivity;
}
