package com.ktb.cafeboo.global.enums;

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
                .orElseThrow(() -> new IllegalArgumentException("Invalid CoffeeChatFilterType: " + status));
    }
}
