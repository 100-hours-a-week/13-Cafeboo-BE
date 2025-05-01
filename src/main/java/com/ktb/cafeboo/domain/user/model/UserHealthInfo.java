package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "user_health_info")
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

    @Column(nullable = false)
    private boolean isPregnant;

    @Column(nullable = false)
    private boolean isTakingBirthPill;

    @Column(nullable = false)
    private boolean isSmoking;

    @Column(nullable = false)
    private boolean hasLiverDisease;

    @Column(nullable = false)
    private LocalTime sleepTime;

    @Column(nullable = false)
    private LocalTime wakeUpTime;

    public void update() {

    }
}
