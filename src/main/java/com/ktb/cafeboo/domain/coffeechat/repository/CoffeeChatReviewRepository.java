package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReview;
import com.ktb.cafeboo.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoffeeChatReviewRepository extends JpaRepository<CoffeeChatReview, Long> {

    Optional<CoffeeChatReview> findByCoffeeChatAndWriter(CoffeeChat chat, User writer);

    boolean existsByCoffeeChatAndWriter(CoffeeChat chat, User writer);
}
