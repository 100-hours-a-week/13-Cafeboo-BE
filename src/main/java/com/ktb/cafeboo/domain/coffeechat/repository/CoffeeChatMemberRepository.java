package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoffeeChatMemberRepository extends JpaRepository<CoffeeChatMember, Long> {

    Optional<CoffeeChatMember> findByCoffeeChatIdAndUserId(Long chatId, Long userId);

    boolean existsByCoffeeChatIdAndChatNickname(Long chatId, String chatNickname);
}
