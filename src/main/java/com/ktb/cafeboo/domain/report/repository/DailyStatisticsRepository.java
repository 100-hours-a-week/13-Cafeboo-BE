package com.ktb.cafeboo.domain.report.repository;

import com.ktb.cafeboo.domain.report.model.DailyStatistics;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyStatisticsRepository extends JpaRepository<DailyStatistics, Long> {
    Optional<DailyStatistics> findByUserIdAndDate(Long userId, LocalDate date);

    // 특정 날짜의 통계 조회
    Optional<DailyStatistics> findByDate(LocalDate date);

    // 특정 기간의 통계 조회
    @Query("SELECT ds FROM DailyStatistics ds WHERE ds.date BETWEEN :startDate AND :endDate")
    List<DailyStatistics> findByDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // 특정 유저의 특정 날짜의 총 카페인 섭취량 조회
    @Query("SELECT ds.totalCaffeineMg FROM DailyStatistics ds WHERE ds.user.id = :userId AND ds.date = :date")
    Optional<Float> findTotalCaffeineByDate(@Param("userId") Long userId, @Param("date") LocalDate date);


    List<DailyStatistics> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
