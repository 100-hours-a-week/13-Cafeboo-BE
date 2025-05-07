package com.ktb.cafeboo.domain.caffeinediary.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.user.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaffeineResidualService {
    private final CaffeineResidualRepository residualRepository;

    private static final Integer HOURS_RANGE = 17;
    private static final double DEFAULT_HALF_LIFE_HOUR = 5.0;
    // 카페인 반감기 (현재는 임의로 5시간으로 설정)
    private double k = Math.log(2) / DEFAULT_HALF_LIFE_HOUR;

    public void modifyResidualAmounts(User user, LocalDateTime previousIntakeTime, float previousCaffeineAmount) {
        final LocalDateTime previousTargetTime = previousIntakeTime.toLocalDate().atStartOfDay();
        final LocalDateTime previousEndTime = previousIntakeTime.plusHours(24);

        // 1. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터 조회 (24hour)
        List<CaffeineResidual> residualsToModify = residualRepository.findByUserAndTargetDateBetween(user, previousTargetTime, previousEndTime);

        // 2. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터에 대해 이전 카페인 섭취량의 영향 제거
        for (CaffeineResidual residual : residualsToModify) {
            LocalDateTime residualDateTime = LocalDateTime.of(
                LocalDate.from(residual.getTargetDate()), LocalTime.of(residual.getHour(), 0));

            // 해당 시점이 이전 섭취 시간과 새로운 섭취 시간 사이에 있는 경우에만 처리
            if (residualDateTime.isAfter(previousIntakeTime) || residualDateTime.isEqual(previousIntakeTime)) {
                // 이전 섭취로 인한 잔존량 계산
                double hoursSincePreviousIntake = ChronoUnit.HOURS.between(previousIntakeTime,
                    residualDateTime);
                double previousResidualAmount =
                    previousCaffeineAmount * Math.exp(-k * hoursSincePreviousIntake);

                // 현재 총 잔존량에서 이전 섭취로 인한 잔존량을 차감
                float updatedAmount =
                    residual.getResidueAmountMg() - (float) previousResidualAmount;

                // 음수가 되지 않도록 보정
                updatedAmount = Math.max(0, updatedAmount);

                residual.setResidueAmountMg(updatedAmount);
            }
        }

        // 3. 변경된 데이터 일괄 저장
        try {
            residualRepository.saveAll(residualsToModify);
        } catch (Exception e) {
            throw new RuntimeException("카페인 잔존량 저장 중 오류가 발생했습니다.", e);
        }
    }

    public void updateResidualAmounts(User user, LocalDateTime intakeTime, float initialCaffeineAmount) {
        LocalDateTime endTime = intakeTime.plusHours(24);

        List<CaffeineResidual> residuals = new ArrayList<>();
        for (int hourOffset = 0; hourOffset <= 24; hourOffset++) {
            LocalDateTime targetTime = intakeTime.plusHours(hourOffset);
            LocalDateTime targetDateTime = targetTime.toLocalDate().atStartOfDay();
            int hour = targetTime.getHour();
            double timeElapsed = hourOffset;
            double residualAmount = initialCaffeineAmount * Math.exp(-k * timeElapsed);

            Optional<CaffeineResidual> existingResidual = residualRepository.findByUserAndTargetDateAndHour(user, targetDateTime, hour);
            double currentCaffeineAtTargetTime;

            if (existingResidual.isPresent()) {
                // 기존 데이터가 있으면, 해당 시점의 잔존량에 현재 섭취량의 영향을 더함
                CaffeineResidual residual = existingResidual.get();
                double previousResidualAmount = residual.getResidueAmountMg();

                // targetTime의 최종 잔존량 (이전 잔존량은 이미 반감기가 적용된 상태)
                currentCaffeineAtTargetTime = previousResidualAmount + residualAmount;

                residual.setResidueAmountMg((float) currentCaffeineAtTargetTime);
                residuals.add(residual);
            } else {
                // 기존 데이터가 없으면 새로 생성
                CaffeineResidual residual = CaffeineResidual.builder()
                    .user(user)
                    .targetDate(targetDateTime)
                    .hour(hour)
                    .residueAmountMg((float) residualAmount)
                    .build();
                residuals.add(residual);
            }
        }
        residualRepository.saveAll(residuals);
    }

    /**
     * 현재 시간 기준 전후 17시간의 카페인 잔존량 데이터를 조회합니다.
     * @param user 조회할 사용자
     * @param currentDateTime 현재 시간
     * @return 35시간 범위의 카페인 잔존량 데이터
     */
    public List<CaffeineResidual> getCaffeineResidualsByTimeRange(
        User user,
        LocalDateTime currentDateTime) {


        LocalDateTime startTime = currentDateTime.minusHours(17);
        LocalDateTime endTime = currentDateTime.plusHours(17);

        log.info("startTime: {}, endTime : {}", startTime, endTime);

        List<CaffeineResidual> residuals = residualRepository.findResidualsByTimeRange(
            user,
            startTime.toLocalDate().atStartOfDay(),
            endTime.toLocalDate().atStartOfDay()
        );

        // 2. Map으로 변환 (key: "yyyy-MM-dd-HH")
        Map<String, CaffeineResidual> residualMap = residuals.stream()
            .collect(Collectors.toMap(
                r -> r.getTargetDate().toLocalDate().toString() + "-" + r.getHour(),
                Function.identity()
            ));

        // 3. 35시간 구간의 모든 시간 포인트 생성 및 매핑
        List<CaffeineResidual> result = new ArrayList<>();
        for (int i = -17; i <= 17; i++) {
            LocalDateTime timePoint = currentDateTime.plusHours(i);
            String key = timePoint.toLocalDate().toString() + "-" + timePoint.getHour();
            CaffeineResidual residual = residualMap.get(key);

            if (residual != null) {
                result.add(residual);
            } else {
                result.add(
                    CaffeineResidual.builder()
                        .user(user)
                        .targetDate(timePoint.toLocalDate().atStartOfDay())
                        .hour(timePoint.getHour())
                        .residueAmountMg(0f)
                        .build()
                );
            }
        }
        return result;
    }
}