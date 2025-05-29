package com.ktb.cafeboo.global.enums;

public enum TokenBlacklistReason {
    LOGOUT,
    REFRESH,
    WITHDRAWAL,
    BAN;

    public String formatWithUserId(String userId) {
        return this.name().toLowerCase() + ":" + userId;
    }
}
