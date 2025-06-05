package com.ktb.cafeboo.domain.coffeechat.dto;

import java.math.BigDecimal;

public record Location(
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String kakaoPlaceUrl
) {}
