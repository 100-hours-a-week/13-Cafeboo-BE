package com.ktb.cafeboo.domain.report.repository;

import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    Optional<WeeklyReport> findByUserIdAndYearAndWeekNum(Long userId, int year, int weekNum);

    List<WeeklyReport> findByUserIdAndYearAndMonth(Long userId, Integer year, Integer month);
}
