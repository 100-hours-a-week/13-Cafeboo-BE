package com.ktb.cafeboo.domain.coffeechat.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String name; // 채팅방 이름 (예: "자유 채팅방", "스터디 그룹")

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

    @Column(name = "kakao_place_url", columnDefinition = "TEXT")
    private String kakaoPlaceUrl;

    public static CoffeeChat of(
            User writer,
            String name,
            LocalDateTime meetingTime,
            int maxMemberCount,
            int currentMemberCount,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String kakaoPlaceUrl
    ) {
        return CoffeeChat.builder()
                .writer(writer)
                .name(name)
                .meetingTime(meetingTime)
                .maxMemberCount(maxMemberCount)
                .currentMemberCount(currentMemberCount)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .kakaoPlaceUrl(kakaoPlaceUrl)
                .build();
    }
}
