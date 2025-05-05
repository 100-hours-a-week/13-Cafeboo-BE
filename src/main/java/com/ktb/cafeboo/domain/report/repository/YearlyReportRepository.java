package com.ktb.cafeboo.domain.report.repository;

import com.ktb.cafeboo.domain.report.model.YearlyReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YearlyReportRepository extends JpaRepository<YearlyReport, Long> {
    Optional<YearlyReport> findByYearAndUserId(Integer year, Long userId);
}
