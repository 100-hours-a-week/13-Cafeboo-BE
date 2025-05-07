package com.ktb.cafeboo.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private String nickname;
    private int dailyCaffeineLimitMg;
    private int coffeeBean;
    private int challengeCount;
}

