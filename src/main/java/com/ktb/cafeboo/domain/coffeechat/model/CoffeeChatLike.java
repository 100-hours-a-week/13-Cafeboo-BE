package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coffee_chat_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "coffee_chat_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoffeeChatLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_chat_id", nullable = false)
    private CoffeeChat coffeeChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static CoffeeChatLike of(CoffeeChat coffeeChat, User user) {
        return CoffeeChatLike.builder()
                .coffeeChat(coffeeChat)
                .user(user)
                .build();
    }
}
