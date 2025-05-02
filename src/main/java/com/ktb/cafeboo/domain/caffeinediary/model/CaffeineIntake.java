package com.ktb.cafeboo.domain.caffeinediary.model;

import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CaffeineIntakes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaffeineIntake extends BaseEntity {

    // 유저 ID
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 섭취 음료 ID
    @ManyToOne
    @JoinColumn(name = "drink_id", nullable = false)
    private Drink drink;

    // 섭취 시간
    private LocalDateTime intakeTime;

    // 섭취 잔 수
    private Integer drinkCount;

    // 섭취 카페인량 (mg)
    private Float caffeineAmountMg;
}
