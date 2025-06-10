package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoffeeChatMemberRepository extends JpaRepository<CoffeeChatMember, Long> {

    Optional<CoffeeChatMember> findByCoffeeChatIdAndUserId(Long chatId, Long userId);

    List<CoffeeChatMember> findAllByCoffeeChat(CoffeeChat chat);

    boolean existsByCoffeeChatAndUser(CoffeeChat chat, User user);

    boolean existsByCoffeeChatIdAndChatNickname(Long chatId, String chatNickname);
}
