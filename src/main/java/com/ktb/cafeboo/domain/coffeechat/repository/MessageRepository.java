package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<CoffeeChatMessage, Long> {

    List<CoffeeChatMessage> findAllByChatOrderByCreatedAtAsc(CoffeeChat chat);
}
