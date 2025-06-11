package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReview;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CoffeeChatReviewRepository extends JpaRepository<CoffeeChatReview, Long> {

    boolean existsByWriter(CoffeeChatMember writer);
}
