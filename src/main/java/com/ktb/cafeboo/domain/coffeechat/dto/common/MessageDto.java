package com.ktb.cafeboo.domain.coffeechat.dto.common;

import java.time.LocalDateTime;

public record MessageDto(
        Long messageId,
        MemberDto sender,
        String content,
        LocalDateTime sentAt
) {}
