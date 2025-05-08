package com.ktb.cafeboo.domain.user.model;

import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_alarm_setting")
public class UserAlarmSetting extends BaseEntity {
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean alarmWhenExceedIntake;

    @Column(nullable = false)
    private boolean alarmBeforeSleep;

    @Column(nullable = false)
    private boolean alarmForChat;
}
