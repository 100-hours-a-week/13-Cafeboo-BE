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
import org.w3c.dom.Text;

@Entity
@Table(name = "MonthlyReports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class MonthlyReport extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "yearly_report_id")
    private YearlyReport yearlyStatisticsId;

    @OneToMany(mappedBy = "monthlyStatisticsId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyReport> weeklyReports = new ArrayList<>();

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
}
