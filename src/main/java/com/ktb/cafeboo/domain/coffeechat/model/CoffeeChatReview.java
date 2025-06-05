package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(
    name = "coffee_chat_reviews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_member_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class CoffeeChatReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private CoffeeChat coffeeChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_member_id", nullable = false)
    private CoffeeChatMember writer;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_img_url", length = 255)
    private String contentImgUrl;

    public static CoffeeChatReview of(CoffeeChat chat, CoffeeChatMember writer, String text, String imgUrl) {
        return CoffeeChatReview.builder()
                .coffeeChat(chat)
                .writer(writer)
                .contentText(text)
                .contentImgUrl(imgUrl)
                .build();
    }
}
