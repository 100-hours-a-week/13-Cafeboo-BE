package com.ktb.cafeboo.domain.tag.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.tag.model.CoffeeChatTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoffeeChatTagRepository extends JpaRepository<CoffeeChatTag, Long> {
    List<CoffeeChatTag> findAllByCoffeeChat(CoffeeChat coffeeChat);
}