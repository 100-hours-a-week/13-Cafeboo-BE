package com.ktb.cafeboo.domain.coffeechat.repository;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
        AND c.meetingTime < :now
        AND c.deletedAt IS NULL
        ORDER BY c.meetingTime DESC
    """)
    List<CoffeeChat> findReviewableChats(Long userId, @Param("now") LocalDateTime now);


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

    // 사용자가 참여했고, 후기가 있는 커피챗 목록
    @Query("""
        SELECT DISTINCT c FROM CoffeeChat c
        JOIN c.members m
        JOIN FETCH c.reviews r
        WHERE m.user.id = :userId
        AND r.deletedAt IS NULL
    """)
    List<CoffeeChat> findChatsWithReviewsByUserId(@Param("userId") Long userId);

    // 후기가 하나 이상 존재하는 모든 커피챗 목록
    @Query("""
        SELECT DISTINCT c FROM CoffeeChat c
        JOIN FETCH c.reviews r
        WHERE r.deletedAt IS NULL
    """)
    List<CoffeeChat> findAllWithReviews();

    @Query("SELECT DISTINCT c FROM CoffeeChat c WHERE c.id = :id")
    @EntityGraph(attributePaths = {
            "reviews.writer",
            "coffeeChatTags.tag"
    })
    Optional<CoffeeChat> findWithDetailsById(Long id);

    @Modifying
    @Query("UPDATE CoffeeChat c " +
            "SET c.status = :toStatus " +
            "WHERE c.meetingTime < :today AND c.status = :fromStatus")
    int expireOutdatedChats(@Param("today") LocalDateTime today,
                            @Param("fromStatus") CoffeeChatStatus fromStatus,
                            @Param("toStatus") CoffeeChatStatus toStatus);

}
