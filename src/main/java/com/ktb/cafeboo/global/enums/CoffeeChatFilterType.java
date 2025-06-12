package com.ktb.cafeboo.global.enums;

import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;

import java.util.Arrays;

public enum CoffeeChatFilterType {
    ALL,
    JOINED,
    ENDED,
    REVIEWABLE;

    public static CoffeeChatFilterType from(String status) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(status))
                .findFirst()
                .orElseThrow(() -> new CustomApiException(ErrorStatus.INVALID_COFFEECHAT_FILTER));
    }
}
