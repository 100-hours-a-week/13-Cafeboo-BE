package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "user_caffeine_info")
public class UserCaffeinInfo extends BaseEntity {
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
    private float dailyCaffeineLimitMg = 100.0f;

    @Column(nullable = false)
    private float sleepSensitiveThresholdMg = 400.0f;

    public void update() {

    }
}
