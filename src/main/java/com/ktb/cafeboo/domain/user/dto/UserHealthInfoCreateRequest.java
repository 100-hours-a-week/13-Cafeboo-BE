package com.ktb.cafeboo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserHealthInfoCreateRequest {

    private String gender;
    private int age;
    private float height;
    private float weight;

    @JsonProperty("isPregnant")
    private Boolean pregnant;

    @JsonProperty("isTakingBirthPill")
    private Boolean takingBirthPill;

    @JsonProperty("isSmoking")
    private Boolean smoking;

    @JsonProperty("hasLiverDisease")
    private Boolean hasLiverDisease;

    @JsonProperty("sleepTime")
    private String sleepTime;

    @JsonProperty("wakeUpTime")
    private String wakeUpTime;

}


