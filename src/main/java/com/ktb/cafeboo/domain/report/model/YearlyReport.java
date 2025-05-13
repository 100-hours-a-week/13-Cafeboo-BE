package com.ktb.cafeboo.domain.report.model;

import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "YearlyReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class YearlyReport extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "yearlyStatisticsId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthlyReport> monthlyStatistics = new ArrayList<>();

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_caffeine_mg", nullable = false)
    private Float totalCaffeineMg;

    @Column(name = "monthly_caffeine_avg_mg", nullable = false)
    private Float monthlyCaffeineAvgMg;
}
