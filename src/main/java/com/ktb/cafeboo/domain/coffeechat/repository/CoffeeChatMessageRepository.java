package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoffeeChatMessageRepository extends JpaRepository<CoffeeChatMessage, Long> {

    List<CoffeeChatMessage> findByCoffeeChatId(Long coffeeChatId);

    List<CoffeeChatMessage> findByCoffeeChatIdAndIdLessThanOrderByIdDesc(Long coffeeChatId, Long cursor, org.springframework.data.domain.Pageable pageable);

    List<CoffeeChatMessage> findByCoffeeChatIdAndIdGreaterThanOrderByIdAsc(Long coffeeChatId, Long cursor, org.springframework.data.domain.Pageable pageable);
}
