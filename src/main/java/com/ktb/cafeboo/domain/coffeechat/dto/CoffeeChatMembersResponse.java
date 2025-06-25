package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.MemberDto;

import java.util.List;

public record CoffeeChatMembersResponse(
        String coffeeChatId,
        int totalMemberCounts,
        List<MemberDto> members
) {}
