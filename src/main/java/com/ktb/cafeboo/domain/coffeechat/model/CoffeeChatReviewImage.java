package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coffee_chat_review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoffeeChatReviewImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private CoffeeChatReview review;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public void setReview(CoffeeChatReview review) {
        this.review = review;
    }

    public static CoffeeChatReviewImage of(CoffeeChatReview review, String imageUrl, Integer sortOrder) {
        return CoffeeChatReviewImage.builder()
                .review(review)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
    }
}
