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
@Table(name = "YearlyReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YearlyReport extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_caffeine_mg", nullable = false)
    private Float totalCaffeineMg;

    @Column(name = "monthly_caffeine_avg_mg", nullable = false)
    private Float monthlyCaffeineAvgMg;

    @Column(name = "ai_message")
    private String aiMessage;
}
