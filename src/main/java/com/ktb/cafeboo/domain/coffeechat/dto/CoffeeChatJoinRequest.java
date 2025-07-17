package com.ktb.cafeboo.domain.coffeechat.dto;


public record CoffeeChatJoinRequest(
        String chatNickname,
        String profileImageType
) {}