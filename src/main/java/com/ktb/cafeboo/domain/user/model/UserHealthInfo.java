package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_health_info")
@Where(clause = "deleted_at IS NULL")
public class UserHealthInfo extends BaseEntity {
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1)
    private String gender;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private float height;

    @Column(nullable = false)
    private float weight;

    @Column(name = "is_pregnant", nullable = false)
    private Boolean pregnant;

    @Column(name = "is_taking_birth_pill", nullable = false)
    private Boolean takingBirthPill;

    @Column(name = "is_smoking", nullable = false)
    private Boolean smoking;

    @Column(nullable = false)
    private Boolean hasLiverDisease;

    @Column(nullable = false)
    private LocalTime sleepTime;

    @Column(nullable = false)
    private LocalTime wakeUpTime;

    public static UserHealthInfo createGuestDefault(User guest) {
        return UserHealthInfo.builder()
                .user(guest)
                .gender("M")
                .age(25)
                .height(175f)
                .weight(70f)
                .pregnant(false)
                .takingBirthPill(false)
                .smoking(false)
                .hasLiverDisease(false)
                .sleepTime(LocalTime.of(0, 0))
                .wakeUpTime(LocalTime.of(0, 0))
                .build();
    }
}
