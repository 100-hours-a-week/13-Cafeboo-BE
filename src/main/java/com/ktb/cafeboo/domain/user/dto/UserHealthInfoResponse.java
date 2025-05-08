package com.ktb.cafeboo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserHealthInfoResponse {
    private String gender;
    private int age;
    private float height;
    private float weight;
    private boolean isPregnant;
    private boolean isTakingBirthPill;
    private boolean isSmoking;
    private boolean hasLiverDisease;
    private String sleepTime;
    private String wakeUpTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
