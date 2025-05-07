package com.ktb.cafeboo.domain.user.dto;

import lombok.Getter;

@Getter
public class UserHealthInfoUpdateRequest {
    private String gender;
    private Integer age;
    private Float height;
    private Float weight;
    private String isPregnant;
    private String isTakingBirthPill;
    private String isSmoking;
    private String hasLiverDisease;
    private String sleepTime;
    private String wakeUpTime;
}
