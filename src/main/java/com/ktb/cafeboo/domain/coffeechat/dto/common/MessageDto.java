package com.ktb.cafeboo.domain.coffeechat.dto.common;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.global.enums.MessageType;
import java.time.LocalDateTime;

public record MessageDto(
        String messageId,
        MemberDto sender,
        String content,
        MessageType messageType,
        LocalDateTime sentAt
) {
    public static MessageDto from(CoffeeChatMessage message, CoffeeChatMember sender) {
        return new MessageDto(
                String.valueOf(message.getId()),
                new MemberDto(
                        String.valueOf(sender.getId()),
                        sender.getChatNickname(),
                        sender.getProfileImageUrl(),
                        sender.isHost()
                ),
                message.getContent(),
                message.getType(),
                message.getCreatedAt()
        );
    }
}
