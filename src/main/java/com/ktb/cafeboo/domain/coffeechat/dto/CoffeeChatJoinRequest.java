package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.global.enums.ProfileImageType;

public record CoffeeChatJoinRequest(
        String chatNickname,
        ProfileImageType profileImageType
) {}