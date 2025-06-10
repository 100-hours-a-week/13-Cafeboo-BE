package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoffeeChatRepository extends JpaRepository<CoffeeChat, Long> {

    // 사용자가 참여중인 채팅방 목록 (JOINED)
    @Query("""
        SELECT c FROM CoffeeChat c
        JOIN CoffeeChatMember m ON c.id = m.coffeeChat.id
        JOIN User u ON m.user.id = u.id
        WHERE m.user.id = :userId
        AND c.status = 'ACTIVE'
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
    """)
    List<CoffeeChat> findJoinedChats(Long userId);

    // 사용자가 참여했고, 시간이 지나 완료된 채팅방 목록 (COMPLETED)
    @Query("""
        SELECT c FROM CoffeeChat c
        JOIN CoffeeChatMember m ON c.id = m.coffeeChat.id
        WHERE m.user.id = :userId
        AND c.status = 'ENDED'
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
    """)
    List<CoffeeChat> findCompletedChats(Long userId);

    @Query("""
        SELECT c FROM CoffeeChat c
        JOIN CoffeeChatMember m ON c.id = m.coffeeChat.id
        WHERE m.user.id = :userId
        AND c.meetingTime < CURRENT_TIMESTAMP
        AND c.deletedAt IS NULL
        ORDER BY c.meetingTime DESC
    """)
    List<CoffeeChat> findReviewableChats(Long userId);


    // 모든 활성화된 채팅방 목록 (ALL)
    @Query("""
        SELECT c FROM CoffeeChat c
        WHERE c.status = 'ACTIVE'
        AND c.deletedAt IS NULL
        ORDER BY c.createdAt DESC
    """)
    List<CoffeeChat> findAllActiveChats();

    // N+1 문제 방지
    @Query("SELECT c FROM CoffeeChat c JOIN FETCH c.members m WHERE c.id = :coffeeChatId")
    Optional<CoffeeChat> findByIdWithMembers(@Param("coffeeChatId") Long coffeeChatId);
}
