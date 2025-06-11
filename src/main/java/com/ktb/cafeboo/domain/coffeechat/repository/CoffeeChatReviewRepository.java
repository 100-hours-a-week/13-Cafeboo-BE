package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReview;
import com.ktb.cafeboo.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoffeeChatReviewRepository extends JpaRepository<CoffeeChatReview, Long> {

    boolean existsByWriter(CoffeeChatMember writer);
}
