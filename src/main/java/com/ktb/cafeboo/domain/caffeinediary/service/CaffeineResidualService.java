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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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

    public void modifyResidualAmounts(User user, LocalDateTime previousIntakeTime, float previousCaffeineAmount) {
        final LocalDateTime previousTargetTime = previousIntakeTime.minusHours(17).toLocalDate().atStartOfDay();
        final LocalDateTime previousEndTime = previousIntakeTime.plusHours(24);

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
            log.error("[CaffeineResidualService.modifyResidualAmounts] 잔존량 수정 저장 실패 - userId={}, message={}", user.getId(), e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.RESIDUAL_SAVE_ERROR);
        }
    }

    public void updateResidualAmounts(User user, LocalDateTime intakeTime, float initialCaffeineAmount) {

        List<CaffeineResidual> residualsToSave = new ArrayList<>();

        // ⭐ 1. 필요한 모든 날짜/시간 범위의 기존 데이터 미리 조회 ⭐
        // 섭취 시간으로부터 24시간 후까지의 모든 날짜 포함 (예: 2025-07-10 14시 ~ 2025-07-11 14시)
        LocalDateTime queryStartDate = intakeTime.toLocalDate().atStartOfDay(); // 섭취일 자정
        LocalDateTime queryEndDate = intakeTime.plusHours(24).toLocalDate().atStartOfDay(); // 24시간 후 날짜의 자정

        // 쿼리 범위를 약간 넓게 잡아서 충분한 데이터를 가져오도록 합니다.
        // 예를 들어 2025-07-10 14시부터 2025-07-11 14시까지의 데이터를 처리해야 한다면,
        // 2025-07-10 00시부터 2025-07-11 23시까지의 데이터를 모두 가져와야 합니다.
        // 여기서는 targetDate가 "날짜의 자정"으로 저장되므로, 날짜 범위만 신경쓰면 됩니다.
        // hour는 0~23까지 모두 조회합니다.
        List<CaffeineResidual> existingResidualsList = residualRepository.findByUserAndTargetDateRangeAndHourRange(
                user,
                queryStartDate,
                queryEndDate.plusDays(1), // 다음 날 자정까지 포함하여 24시간 넘는 구간 커버
                0, // 모든 시간
                23 // 모든 시간
        );

        // ⭐ 2. 조회된 데이터를 Map으로 변환하여 O(1) 탐색 가능하게 함 ⭐
        Map<String, CaffeineResidual> existingResidualsMap = new HashMap<>();
        for (CaffeineResidual res : existingResidualsList) {
            // Map 키는 targetDate (YYYY-MM-DD 00:00:00) + hour 조합으로 만듭니다.
            String key = res.getTargetDate().toLocalDate().toString() + "_" + res.getHour();
            existingResidualsMap.put(key, res);
        }

        // ⭐ 3. 루프 내에서는 Map에서 데이터 조회/처리 ⭐
        for (int hourOffset = 0; hourOffset <= 24; hourOffset++) {
            LocalDateTime currentCalculatedTime = intakeTime.plusHours(hourOffset);
            LocalDateTime dbCompliantTargetDate = currentCalculatedTime.toLocalDate().atStartOfDay();
            int hourPart = currentCalculatedTime.getHour();
            double timeElapsed = hourOffset;
            double residualAmountFromThisIntake = initialCaffeineAmount * Math.exp(-k * timeElapsed);

            String key = dbCompliantTargetDate.toLocalDate().toString() + "_" + hourPart; // Map 조회 키

            CaffeineResidual residualToProcess;

            if (existingResidualsMap.containsKey(key)) {
                residualToProcess = existingResidualsMap.get(key);
                double previousTotalResidual = residualToProcess.getResidueAmountMg();
                double newTotalResidual = previousTotalResidual + residualAmountFromThisIntake;
                residualToProcess.setResidueAmountMg((float) newTotalResidual);
            } else {
                residualToProcess = CaffeineResidual.builder()
                        .user(user)
                        .targetDate(dbCompliantTargetDate)
                        .hour(hourPart)
                        .residueAmountMg((float) residualAmountFromThisIntake)
                        .build();
                // 새로 생성된 객체는 Map에도 추가하여, 만약 루프 안에서 같은 키를 두 번 처리할 일이 생긴다면
                // (이 로직에서는 발생하지 않겠지만) 중복 생성을 방지합니다.
                existingResidualsMap.put(key, residualToProcess);
            }
            residualsToSave.add(residualToProcess);
        }

        try {
            residualRepository.saveAll(residualsToSave);
        } catch (DataIntegrityViolationException e) {
            // 이 오류가 발생한다면, DB에 중복 데이터가 남아있거나 인덱스가 제대로 동작하지 않는 것
            log.error("[CaffeineResidualService.updateResidualAmounts] 잔존량 갱신 중 DB 무결성 위반: userId={}, message={}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("카페인 잔존량 갱신 중 데이터 무결성 오류가 발생했습니다. DB 상태를 확인해주세요. (중복 데이터)", e);
        } catch (Exception e) {
            log.error("[CaffeineResidualService.updateResidualAmounts] 잔존량 갱신 저장 실패 - userId={}, message={}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("카페인 잔존량 갱신 중 예상치 못한 오류가 발생했습니다.", e);
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