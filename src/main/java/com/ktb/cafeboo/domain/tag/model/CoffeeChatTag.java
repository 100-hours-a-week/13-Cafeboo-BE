package com.ktb.cafeboo.domain.tag.model;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coffee_chat_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoffeeChatTag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private CoffeeChat coffeeChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
