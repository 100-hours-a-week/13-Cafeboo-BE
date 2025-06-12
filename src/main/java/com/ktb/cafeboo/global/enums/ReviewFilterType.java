package com.ktb.cafeboo.global.enums;

import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewFilterType {
    ALL, MY;

    public static ReviewFilterType from(String input) {
        try {
            return ReviewFilterType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomApiException(ErrorStatus.INVALID_REVIEW_FILTER);
        }
    }
}
