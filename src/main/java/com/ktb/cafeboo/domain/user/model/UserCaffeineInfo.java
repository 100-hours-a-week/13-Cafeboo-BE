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
@Table(name = "user_caffeine_info")
@Where(clause = "deleted_at IS NULL")
public class UserCaffeineInfo extends BaseEntity {
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private int caffeineSensitivity = 0;

    @Column
    private float averageDailyCaffeineIntake;

    @Column
    private LocalTime frequentDrinkTime;

    @Column(nullable = false)
    private float dailyCaffeineLimitMg = 400.0f;

    @Column(nullable = false)
    private float sleepSensitiveThresholdMg = 100.0f;

}
