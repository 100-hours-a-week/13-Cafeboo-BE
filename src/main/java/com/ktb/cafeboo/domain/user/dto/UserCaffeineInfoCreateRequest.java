package com.ktb.cafeboo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class UserCaffeineInfoCreateRequest {
    private int caffeineSensitivity;
    private float averageDailyCaffeineIntake;
    private String frequentDrinkTime;
    private List<String> userFavoriteDrinks;
}
