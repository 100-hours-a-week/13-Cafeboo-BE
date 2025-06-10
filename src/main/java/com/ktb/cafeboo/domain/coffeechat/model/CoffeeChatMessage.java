package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeChatMessage extends BaseEntity {

    @Column(name = "message_uuid", nullable = false, unique = true, length = 36)
    private String messageUuid; // 클라이언트가 생성한 UUID 메시지 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private CoffeeChat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private CoffeeChatMember sender;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private MessageType type;

    public static CoffeeChatMessage of(CoffeeChat chat, CoffeeChatMember sender, String content, MessageType type) {
        return CoffeeChatMessage.builder()
            .chat(chat)
            .sender(sender)
            .content(content)
            .type(type)
            .build();
    }
}