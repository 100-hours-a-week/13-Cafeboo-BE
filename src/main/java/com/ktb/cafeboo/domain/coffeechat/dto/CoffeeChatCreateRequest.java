package com.ktb.cafeboo.domain.coffeechat.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CoffeeChatCreateRequest(
        String title,
        String content,
        LocalDate date,
        LocalTime time,
        int memberCount,
        List<String> tags,
        Location location
) {}
