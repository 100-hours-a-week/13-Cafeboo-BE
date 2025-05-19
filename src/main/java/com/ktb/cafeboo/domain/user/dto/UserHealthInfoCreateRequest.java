package com.ktb.cafeboo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserHealthInfoCreateRequest(
        String gender,
        int age,
        float height,
        float weight,
        @JsonProperty("isPregnant") Boolean pregnant,
        @JsonProperty("isTakingBirthPill") Boolean takingBirthPill,
        @JsonProperty("isSmoking") Boolean smoking,
        @JsonProperty("hasLiverDisease") Boolean hasLiverDisease,
        @JsonProperty("sleepTime") String sleepTime,
        @JsonProperty("wakeUpTime") String wakeUpTime
) {}