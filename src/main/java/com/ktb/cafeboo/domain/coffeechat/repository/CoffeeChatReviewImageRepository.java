package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoffeeChatReviewImageRepository extends JpaRepository<CoffeeChatReviewImage, Long> {
    @Query("""
        SELECT ri FROM CoffeeChatReviewImage ri
        WHERE ri.review.id IN :reviewIds
        ORDER BY ri.sortOrder
    """)
    List<CoffeeChatReviewImage> findAllByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
