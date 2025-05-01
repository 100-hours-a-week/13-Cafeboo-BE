package com.ktb.cafeboo.domain.caffeinediary.repository;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.user.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CaffeineResidualRepository extends JpaRepository<CaffeineResidual, Long> {
    Optional<CaffeineResidual> findByUserAndTargetDateAndHour(User user, LocalDate targetDate, int hour);

    @Query("SELECT cr FROM CaffeineResidual cr " +
        "WHERE cr.user = :user " +
        "AND cr.targetDate BETWEEN :startDate AND :endDate")
    List<CaffeineResidual> findByUserAndTargetDateBetween(
        @Param("user") User user,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 현재 시간 기준 전후 35시간의 카페인 잔존량 데이터를 조회합니다.
     */
    @Query("SELECT cr FROM CaffeineResidual cr " +
        "WHERE cr.user = :user " +
        "AND ((cr.targetDate = :previousDate AND cr.hour >= :previousStartHour) " +
        "     OR (cr.targetDate = :currentDate) " +
        "     OR (cr.targetDate = :nextDate AND cr.hour <= :nextEndHour)) " +
        "ORDER BY cr.targetDate ASC, cr.hour ASC")
    List<CaffeineResidual> findResidualsByTimeRange(
        @Param("user") User user,
        @Param("previousDate") LocalDate previousDate,
        @Param("currentDate") LocalDate currentDate,
        @Param("nextDate") LocalDate nextDate,
        @Param("previousStartHour") int previousStartHour,
        @Param("nextEndHour") int nextEndHour
    );
}
