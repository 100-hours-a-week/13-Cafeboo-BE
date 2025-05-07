package com.ktb.cafeboo.global.util;

import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;

public class AuthChecker {

    public static void checkOwnership(Long ownerId, Long currentUserId) {
        if (!ownerId.equals(currentUserId)) {
            throw new CustomApiException(ErrorStatus.ACCESS_DENIED);
        }
    }
}
