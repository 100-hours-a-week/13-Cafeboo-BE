package com.ktb.cafeboo.domain.caffeinediary.service;

import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
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
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaffeineResidualService {
    private final CaffeineResidualRepository residualRepository;

    private final UserService userService;

    private static final Integer HOURS_RANGE = 17;
    private static final double DEFAULT_HALF_LIFE_HOUR = 5.0;
    // 카페인 반감기 (현재는 임의로 5시간으로 설정)
    private double k = Math.log(2) / DEFAULT_HALF_LIFE_HOUR;

    public void modifyResidualAmounts(Long userId, LocalDateTime previousIntakeTime, float previousCaffeineAmount) {
        final LocalDateTime previousTargetTime = previousIntakeTime.minusHours(17).toLocalDate().atStartOfDay();
        final LocalDateTime previousEndTime = previousIntakeTime.plusHours(24);

        User user = userService.findUserById(userId);
        LocalDateTime previousIntakeHour = previousIntakeTime.truncatedTo(ChronoUnit.HOURS);
        log.info("[CaffeineResidualService.modifyResidualAmounts] 수정 대상 기준 섭취 시간(hour 단위) - previousIntakeHour={}", previousIntakeHour);
        // 1. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터 조회 (24hour)
        List<CaffeineResidual> residualsToModify = residualRepository.findByUserAndTargetDateBetween(user, previousTargetTime, previousEndTime);

        // Comparator를 사용하여 정렬
        Comparator<CaffeineResidual> comparator = Comparator
            .comparing(CaffeineResidual::getTargetDate) // targetDate의 날짜 부분으로 먼저 비교
            .thenComparing(CaffeineResidual::getHour); // 그 다음 hour 값으로 비교

        // 리스트 정렬
        residualsToModify.sort(comparator);

        int hoursSincePreviousIntake = 0;
        // 2. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터에 대해 이전 카페인 섭취량의 영향 제거
        for (CaffeineResidual residual : residualsToModify) {
            LocalDateTime residualDateTime = LocalDateTime.of(
                LocalDate.from(residual.getTargetDate()), LocalTime.of(residual.getHour(), 0));

            // 해당 시점이 이전 섭취 시간과 새로운 섭취 시간 사이에 있는 경우에만 처리
            if (residualDateTime.isAfter(previousIntakeHour) || residualDateTime.isEqual(previousIntakeHour)) {
                // 이전 섭취로 인한 잔존량 계산
                log.info("[modifyResidualAmounts] 잔존량 계산 대상 시간 비교 - residualDateTime={}, 기준섭취시간={}, isAfter={}, isEqual={}",
                        residualDateTime, previousIntakeHour,
                        residualDateTime.isAfter(previousIntakeHour),
                        residualDateTime.isEqual(previousIntakeHour));
                double previousResidualAmount =
                    previousCaffeineAmount * Math.exp(-k * hoursSincePreviousIntake);

                // 현재 총 잔존량에서 이전 섭취로 인한 잔존량을 차감
                float updatedAmount =
                    residual.getResidueAmountMg() - (float) previousResidualAmount;

                // 음수가 되지 않도록 보정
                updatedAmount = Math.max(0, updatedAmount);

                residual.setResidueAmountMg(updatedAmount);

                hoursSincePreviousIntake++;
            }
        }

        // 3. 변경된 데이터 일괄 저장
        try {
            residualRepository.saveAll(residualsToModify);
        } catch (Exception e) {
            log.error("[CaffeineResidualService.modifyResidualAmounts] 잔존량 수정 저장 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.RESIDUAL_SAVE_ERROR);
        }
    }

    public void updateResidualAmounts(Long userId, LocalDateTime intakeTime, float initialCaffeineAmount) {
        User user = userService.findUserById(userId);

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
        try {
            residualRepository.saveAll(residuals);
        } catch (Exception e) {
            log.error("[CaffeineResidualService.updateResidualAmounts] 잔존량 갱신 저장 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new RuntimeException("카페인 잔존량 갱신 중 오류가 발생했습니다.", e);
        }
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

        LocalDateTime startTime = currentDateTime.minusHours(HOURS_RANGE);
        LocalDateTime endTime = currentDateTime.plusHours(HOURS_RANGE);

        List<CaffeineResidual> residuals = residualRepository.findResidualsByTimeRange(
            user,
            startTime.toLocalDate().atStartOfDay(),
            endTime.toLocalDate().atStartOfDay()
        );

        // 2. Map으로 변환 (key: "yyyy-MM-dd-HH")
        Map<String, CaffeineResidual> residualMap = residuals.stream()
            .collect(Collectors.toMap(
                r -> r.getTargetDate().toLocalDate().toString() + "-" + r.getHour(),
                Function.identity(),
                (r1, r2) -> r2  // 중복된 경우 나중 값으로 덮어쓰기
            ));

        // 3. 35시간 구간의 모든 시간 포인트 생성 및 매핑
        List<CaffeineResidual> result = new ArrayList<>();
        for (int i = -1 * HOURS_RANGE; i <= HOURS_RANGE; i++) {
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

    public CaffeineResidual findByUserAndTargetDateAndHour(User user, LocalDateTime now, int hour){
        Optional<CaffeineResidual> residualOptional = residualRepository.findByUserAndTargetDateAndHour(user, now, hour);

        return residualOptional.orElseGet(() -> CaffeineResidual.builder()
            .user(user)
            .targetDate(now)
            .hour(hour)
            .residueAmountMg(0.0f)
            .build());
    }
}