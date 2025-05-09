package com.ktb.cafeboo.domain.report.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "WeeklyReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class WeeklyReport extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "monthly_statistics_id", nullable = false)
    private MonthlyReport monthlyReport;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "week_num", nullable = false)
    private Integer weekNum;

    @Column(name = "total_caffeine_mg", nullable = false)
    private Float totalCaffeineMg;

    @Column(name = "daily_caffeine_avg_mg", nullable = false)
    private Float dailyCaffeineAvgMg;

    @Column(name = "over_intake_days", nullable = false)
    private Integer overIntakeDays;

    @Column(name = "ai_message")
    private String aiMessage;
}