package com.ktb.cafeboo.domain.report.model;

import com.ktb.cafeboo.domain.drink.model.Cafe;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "DailyStatistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStatistics extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "weekly_statistics_id", nullable = false)
    private Long weeklyStatisticsId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_caffeine_mg", nullable = false)
    private Float totalCaffeineMg;

}