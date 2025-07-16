package com.ktb.cafeboo.domain.caffeinediary.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(
        name = "CaffeineResiduals",
        indexes = {
                @Index(name = "idx_user_target_hour", columnList = "user_id, targetDate, hour")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class CaffeineResidual extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 기준 날짜
    private LocalDateTime targetDate;

    // 기준 시간 (0~23시)
    private Integer hour;

    // 잔존량 (mg)
    private Float residueAmountMg;
}
