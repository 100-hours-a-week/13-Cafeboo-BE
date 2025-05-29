package com.ktb.cafeboo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserHealthInfoUpdateRequest(
        String gender,
        Integer age,
        Float height,
        Float weight,
        @JsonProperty("isPregnant") Boolean pregnant,
        @JsonProperty("isTakingBirthPill") Boolean takingBirthPill,
        @JsonProperty("isSmoking") Boolean smoking,
        @JsonProperty("hasLiverDisease") Boolean hasLiverDisease,
        @JsonProperty("sleepTime") String sleepTime,
        @JsonProperty("wakeUpTime") String wakeUpTime
) {}