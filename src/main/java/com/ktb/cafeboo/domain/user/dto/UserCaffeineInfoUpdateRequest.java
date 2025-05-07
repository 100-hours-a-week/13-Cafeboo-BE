package com.ktb.cafeboo.domain.user.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class UserCaffeineInfoUpdateRequest {
    private Integer caffeineSensitivity;
    private Float averageDailyCaffeineIntake;
    private String frequentDrinkTime;
    private List<String> userFavoriteDrinks; // 현재 미사용
}
