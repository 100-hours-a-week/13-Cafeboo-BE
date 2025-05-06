package com.ktb.cafeboo.domain.report.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MonthlyReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReport extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "yearly_report_id")
    private YearlyReport yearlyStatisticsId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "total_caffeine_mg", nullable = false)
    private float totalCaffeineMg;

    @Column(name = "weekly_caffeine_avg_mg", nullable = false)
    private float weeklyCaffeineAvgMg;

    @Column(name = "ai_message")
    private String aiMessage;
}
