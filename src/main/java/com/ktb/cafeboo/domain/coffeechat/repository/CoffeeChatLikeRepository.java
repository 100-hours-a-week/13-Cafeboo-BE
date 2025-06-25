package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatLike;
import com.ktb.cafeboo.global.enums.CoffeeChatLikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoffeeChatLikeRepository extends JpaRepository<CoffeeChatLike, Long> {

    Optional<CoffeeChatLike> findByCoffeeChatIdAndUserId(Long coffeeChatId, Long userId);

    boolean existsByCoffeeChatIdAndUserIdAndStatus(Long coffeeChatId, Long userId, CoffeeChatLikeStatus status);
}
