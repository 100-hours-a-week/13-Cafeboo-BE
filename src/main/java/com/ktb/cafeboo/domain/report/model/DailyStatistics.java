package com.ktb.cafeboo.domain.report.model;

import com.ktb.cafeboo.domain.drink.model.Cafe;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDate;

@Entity
@Table(name = "DailyStatistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class DailyStatistics extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "weekly_statistics_id", nullable = false)
    private WeeklyReport weeklyStatisticsId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_caffeine_mg", nullable = false)
    private Float totalCaffeineMg;

    @Lob
    @Column(name = "ai_message", columnDefinition = "TEXT")
    private String aiMessage;
}