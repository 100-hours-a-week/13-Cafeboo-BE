package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.MessageDto;
import lombok.Builder;

import java.util.List;

@Builder
public record CoffeeChatMessagesResponse(
        String coffeeChatId,
        List<MessageDto> messages,
        String nextCursor,
        boolean hasNext
) {}
