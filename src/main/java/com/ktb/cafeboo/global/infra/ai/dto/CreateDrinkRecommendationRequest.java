package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateDrinkRecommendationRequest {
    private String gender;
    private int age;
    private float height;
    private float weight;
    private int isPregnant;
    private int isTakingBirthPill;
    private int isSmoking;
    private int caffeineSensitivity;
    private float avgIntakePerDay;
    private float avgCaffeineAmount;
    private float dailyCaffeineLimit;
}
