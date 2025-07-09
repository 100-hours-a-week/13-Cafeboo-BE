package com.ktb.cafeboo.domain.tag.repository;

import com.ktb.cafeboo.domain.tag.model.CoffeeChatTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CoffeeChatTagRepository extends JpaRepository<CoffeeChatTag, Long> {
    @Query("""
        SELECT cct FROM CoffeeChatTag cct
        WHERE cct.coffeeChat.id IN :chatIds
    """)
    List<CoffeeChatTag> findByChatIds(List<Long> chatIds);

}