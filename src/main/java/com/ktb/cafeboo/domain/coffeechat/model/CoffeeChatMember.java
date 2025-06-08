package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.CoffeeChatMemberStatus;
import com.ktb.cafeboo.global.enums.ProfileImageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "coffee_chat_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class CoffeeChatMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private CoffeeChat coffeeChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "chat_nickname", nullable = false, length = 20)
    private String chatNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_image_type", nullable = false, length = 10)
    private ProfileImageType profileImageType;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CoffeeChatMemberStatus status;

    @Column(name = "is_evaluated", nullable = false)
    private boolean isEvaluated;

    public static CoffeeChatMember of(
            CoffeeChat chat,
            User user,
            String chatNickname,
            ProfileImageType profileImageType
    ) {
        return CoffeeChatMember.builder()
                .coffeeChat(chat)
                .user(user)
                .chatNickname(chatNickname)
                .profileImageType(profileImageType)
                .status(CoffeeChatMemberStatus.JOINED)
                .isEvaluated(false)
                .build();
    }
}