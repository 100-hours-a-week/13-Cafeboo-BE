package com.ktb.cafeboo.domain.coffeechat.dto.common;

import com.ktb.cafeboo.global.enums.MessageType;
import java.time.LocalDateTime;

public record MessageDto(
        String messageId,
        MemberDto sender,
        String content,
        MessageType messageType,
        LocalDateTime sentAt
) {}
