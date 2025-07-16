package com.ktb.cafeboo.domain.caffeinediary.repository;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.user.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CaffeineResidualRepository extends JpaRepository<CaffeineResidual, Long> {
    Optional<CaffeineResidual> findByUserAndTargetDateAndHour(User user, LocalDateTime targetDate, int hour);

    @Query("SELECT cr FROM CaffeineResidual cr " +
        "WHERE cr.user = :user " +
        "AND cr.targetDate BETWEEN :startDate AND :endDate")
    List<CaffeineResidual> findByUserAndTargetDateBetween(
        @Param("user") User user,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 현재 시간 기준 전후 35시간의 카페인 잔존량 데이터를 조회합니다.
     */
    @Query("SELECT r FROM CaffeineResidual r WHERE r.user = :user AND r.targetDate BETWEEN :startTime AND :endTime ORDER BY r.targetDate ASC")
    List<CaffeineResidual> findResidualsByTimeRange(
        @Param("user") User user,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // 특정 사용자, 특정 날짜 범위, 특정 시간 범위에 해당하는 모든 잔존량 데이터를 가져오는 쿼리
    @Query("SELECT cr FROM CaffeineResidual cr " +
            "WHERE cr.user = :user " +
            "AND cr.targetDate BETWEEN :startDate AND :endDate " + // 날짜 범위 (시간 00:00:00 기준)
            "AND cr.hour BETWEEN :startHour AND :endHour " +
            "AND cr.deletedAt IS NULL")
    List<CaffeineResidual> findByUserAndTargetDateRangeAndHourRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("startHour") Integer startHour,
            @Param("endHour") Integer endHour
    );
}
