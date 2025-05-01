package com.ktb.cafeboo.domain.caffeinediary.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CaffeineResiduals")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaffeineResidual extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 기준 날짜 (날짜만)
    private LocalDate targetDate;

    // 기준 시간 (0~23시)
    private Integer hour;

    // 잔존량 (mg)
    private Float residueAmountMg;
}
