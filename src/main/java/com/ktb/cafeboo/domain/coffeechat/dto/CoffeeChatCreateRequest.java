package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.global.enums.ProfileImageType;

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
        LocationDto location,
        String chatNickname,
        ProfileImageType profileImageType
) {}
