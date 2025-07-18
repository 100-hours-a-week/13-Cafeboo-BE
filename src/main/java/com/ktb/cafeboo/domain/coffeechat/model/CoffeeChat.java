package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.domain.tag.model.CoffeeChatTag;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coffee_chats")
@Where(clause = "deleted_at IS NULL")
public class CoffeeChat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User writer;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CoffeeChatStatus status;

    @Column(name = "meeting_time", nullable = false)
    private LocalDateTime meetingTime;

    @Column(name = "max_member_count", nullable = false)
    private int maxMemberCount;

    @Column(name = "current_member_count", nullable = false)
    private int currentMemberCount;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "kakao_place_url", length = 512)
    private String kakaoPlaceUrl;

    @Builder.Default
    @OneToMany(mappedBy = "coffeeChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoffeeChatMember> members = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "coffeeChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoffeeChatMessage> messages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "coffeeChat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<CoffeeChatReview> reviews = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "coffeeChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CoffeeChatTag> coffeeChatTags = new HashSet<>();

    @Column(name = "likes_count", nullable = false)
    private int likesCount = 0;

    @OneToMany(mappedBy = "coffeeChat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoffeeChatLike> likes = new ArrayList<>();

    public void increaseLikes() {
        this.likesCount++;
    }

    public void decreaseLikes() {
        if (this.likesCount > 0) this.likesCount--;
    }

    public void addMember(CoffeeChatMember member) {
        this.members.add(member);
        this.currentMemberCount += 1;
    }

    public void removeMember(CoffeeChatMember member) {
        this.members.remove(member);
        this.currentMemberCount -= 1;
    }

    public boolean isJoinedBy(Long userId) {
        return members.stream().anyMatch(m -> m.getUser().getId().equals(userId));
    }

    public boolean isReviewedBy(Long userId) {
        return reviews.stream()
                .anyMatch(r -> r.getWriter().getUser().getId().equals(userId));
    }

    public List<String> getTagNames() {
        return coffeeChatTags.stream()
                .map(coffeeChatTag -> coffeeChatTag.getTag().getName())
                .toList();
    }

    public void softDelete() {
        this.status = CoffeeChatStatus.DELETED;
        this.delete();
    }
}
