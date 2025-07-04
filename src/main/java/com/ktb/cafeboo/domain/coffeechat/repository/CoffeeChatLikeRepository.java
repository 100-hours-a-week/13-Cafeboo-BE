package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatLike;
import com.ktb.cafeboo.global.enums.CoffeeChatLikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CoffeeChatLikeRepository extends JpaRepository<CoffeeChatLike, Long> {

    Optional<CoffeeChatLike> findByCoffeeChatIdAndUserId(Long coffeeChatId, Long userId);

    boolean existsByCoffeeChatIdAndUserIdAndStatus(Long coffeeChatId, Long userId, CoffeeChatLikeStatus status);

    @Query("""
        SELECT cl FROM CoffeeChatLike cl
        WHERE cl.user.id = :userId
        AND cl.status = 'ACTIVE'
        AND cl.coffeeChat.id IN :chatIds
    """)
    List<CoffeeChatLike> findLikesByUserAndChatIds(Long userId, List<Long> chatIds);
}
