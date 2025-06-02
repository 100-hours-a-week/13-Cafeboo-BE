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
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private CoffeeChat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, length = 300)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private MessageType type;

    public static Message of(CoffeeChat chat, User sender, String content, MessageType type) {
        return Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .type(type)
                .build();
    }
}
