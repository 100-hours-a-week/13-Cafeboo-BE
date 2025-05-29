package com.ktb.cafeboo.domain.user.dto;

public record EmailDuplicationResponse(
        String email,
        boolean isDuplicated
) {}
