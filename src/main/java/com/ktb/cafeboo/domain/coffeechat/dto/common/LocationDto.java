package com.ktb.cafeboo.domain.coffeechat.dto.common;

import java.math.BigDecimal;

public record LocationDto(
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String kakaoPlaceUrl
) {}
