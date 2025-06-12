package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

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
    private String text;

    @Builder.Default
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CoffeeChatReviewImage> images = new ArrayList<>();

    public static CoffeeChatReview of(CoffeeChat chat, CoffeeChatMember writer, String text, List<CoffeeChatReviewImage> imgUrls) {
        CoffeeChatReview review = CoffeeChatReview.builder()
                .coffeeChat(chat)
                .writer(writer)
                .text(text)
                .build();

        for (CoffeeChatReviewImage img : imgUrls) {
            img.setReview(review);
        }
        review.getImages().addAll(imgUrls);
        return review;
    }
}
