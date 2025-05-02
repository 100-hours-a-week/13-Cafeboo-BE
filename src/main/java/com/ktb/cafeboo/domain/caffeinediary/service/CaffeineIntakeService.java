package com.ktb.cafeboo.domain.caffeinediary.service;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineResidual;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.drink.repository.DrinkRepository;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
@Transactional
public class CaffeineIntakeService {

    private final CaffeineIntakeRepository intakeRepository;
    private final CaffeineResidualRepository residualRepository;
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;
    private final DailyStatisticsService dailyStatisticsService;

    private static final double DEFAULT_HALF_LIFE_HOUR = 5.0; // 평균 반감기

    // 카페인 반감기 (현재는 임의로 5시간으로 설정)
    private double k = Math.log(2) / DEFAULT_HALF_LIFE_HOUR;

    /**
     * 음료 정보를 조회하고 캐싱합니다.
     * @param drinkId 음료 ID
     * @return 조회된 음료 객체
     * @throws IllegalArgumentException 해당 ID의 음료가 없을 경우 발생
     */
    @Cacheable(value = "drinks", key = "#drinkId") // drinks라는 캐시에 drinkId를 키로 사용하여 캐싱
    public Drink getDrink(Long drinkId) {
        return drinkRepository.findById(drinkId)
            .orElseThrow(() -> new IllegalArgumentException("음료 정보가 없습니다."));
    }

    /**
     * 새로운 카페인 섭취 기록을 등록하고, 섭취 시간을 기준으로 사용자의 예상 카페인 잔존량을 계산하여 데이터베이스에 업데이트합니다.
     *
     * @param user    사용자 정보 객체
     * @param request 요청으로 보낸 drink 정보
     * @return CaffeineIntakeResponse: 생성된 카페인 섭취 기록에 대한 응답
     * @throws IllegalArgumentException 유저 정보 또는 음료 정보가 없을 경우 발생합니다.
     */
    public CaffeineIntakeResponse recordCaffeineIntake(User user, CaffeineIntakeRequest request) {
        // 0. 데이터 유효성 겁사. 사용자 정보, 음료 정보가 유효하지 않을 시 IllegalArgumentException 발생

        Drink drink = getDrink(request.getDrinkId());

        // 1. 섭취 정보 저장
        CaffeineIntake intake = CaffeineIntake.builder()
            .user(user)
            .drink(drink)
            .intakeTime(request.getIntakeTime())
            .drinkCount(request.getDrinkCount())
            .caffeineAmountMg(request.getCaffeineAmount())
            .build();
        intakeRepository.save(intake);

        // 2. 잔존량 계산
       updateResidualAmounts(user, request.getIntakeTime(), request.getCaffeineAmount());

//        // 3. DailyStatistics 업데이트
//        dailyStatisticsService.updateDailyStatistics(1L, request.getCaffeineAmount(), 1L);

        // 4. 응답 DTO 생성 및 반환
        return CaffeineIntakeResponse.builder()
            .id(intake.getId())
            .drinkId(request.getDrinkId())
            .drinkName(drink.getName())
            .intakeTime(request.getIntakeTime())
            .drinkCount(request.getDrinkCount())
            .caffeineAmount(request.getCaffeineAmount())
            .build();
    }

    /**
     * 카페인 섭취 기록 ID로 특정 섭취 기록을 조회합니다.
     * @param intakeId 조회할 카페인 섭취 기록 ID
     * @return 조회된 카페인 섭취 기록
     * @throws IllegalArgumentException 해당 ID의 섭취 기록이 없을 경우 발생
     */
    public CaffeineIntake getCaffeineIntakeById(Long intakeId) {
        return intakeRepository.findById(intakeId)
            .orElseThrow(() -> new IllegalArgumentException("해당 ID의 카페인 섭취 기록이 없습니다."));
    }

    /**
     * 카페인 섭취 기록을 수정합니다. 요청에 포함된 필드만 수정합니다.
     * @param intakeId 수정할 카페인 섭취 기록 ID
     * @param request 수정할 내용을 담은 DTO
     * @return 수정된 카페인 섭취 기록에 대한 응답
     * @throws IllegalArgumentException 해당 ID의 섭취 기록이 없거나, 유효하지 않은 음료 ID가 입력된 경우 발생
     */
    public CaffeineIntakeResponse updateCaffeineIntake(Long intakeId, CaffeineIntakeRequest request) {
        // 1. 수정할 섭취 기록 조회
        CaffeineIntake intake = getCaffeineIntakeById(intakeId);
        User user = intake.getUser();
        LocalDateTime previousIntakeTime = intake.getIntakeTime();
        float previousCaffeineAmount = intake.getCaffeineAmountMg();
        int previousDrinkCount = intake.getDrinkCount();

        // 2. 수정할 필드 적용
        if (request.getDrinkId() != null) {
            Drink drink = drinkRepository.findById(request.getDrinkId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 음료 정보입니다."));
            intake.setDrink(drink);
        }
        if (request.getIntakeTime() != null) {
            intake.setIntakeTime(request.getIntakeTime());
        }
        if (request.getDrinkCount() != null) {
            intake.setDrinkCount(request.getDrinkCount());
        }
        if (request.getCaffeineAmount() != null) {
            intake.setCaffeineAmountMg(request.getCaffeineAmount());
        }

        // 3. 연관된 잔존량 정보 수정. 수정된 섭취 시간, 음료 잔 수에 따라 잔존량 정보를 다시 계산해야 함
        float newCaffeineAmount = request.getCaffeineAmount() != null ? request.getCaffeineAmount() : intake.getCaffeineAmountMg();
        int newDrinkCount = request.getDrinkCount() != null ? request.getDrinkCount() : intake.getDrinkCount();
        LocalDateTime newIntakeTime = request.getIntakeTime() != null ? request.getIntakeTime() : intake.getIntakeTime();

        // 기존 시간 기준 삭제 ->  수정 이전의 잔존량 삭제
        modifyResidualAmounts(user, previousIntakeTime, previousCaffeineAmount * previousDrinkCount);

        // 새로운 시간 기준으로 update -> 수정 후의 잔존량 계산 및 저장
        updateResidualAmounts(user, newIntakeTime, newCaffeineAmount * newDrinkCount);


        // 4. 수정된 섭취 기록 반환
        return CaffeineIntakeResponse.builder()
            .id(intake.getId())
            .drinkId(intake.getDrink().getId())
            .drinkName(intake.getDrink().getName())
            .intakeTime(intake.getIntakeTime())
            .drinkCount(intake.getDrinkCount())
            .caffeineAmount(intake.getCaffeineAmountMg())
            .build();
    }

    private void modifyResidualAmounts(User user, LocalDateTime previousIntakeTime, float previousCaffeineAmount) {
        final LocalDate previousTargetDate = previousIntakeTime.toLocalDate();
        final LocalDateTime previousEndTime = previousIntakeTime.plusHours(24);

        // 1. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터 조회 (24hour)
        List<CaffeineResidual> residualsToModify = residualRepository.findByUserAndTargetDateBetween(user, previousTargetDate, previousEndTime.toLocalDate());

        // 2. 섭취 내역 수정으로 인해 영향을 받는 잔존량 데이터에 대해 이전 카페인 섭취량의 영향 제거
        for (CaffeineResidual residual : residualsToModify) {
            LocalDateTime residualDateTime = LocalDateTime.of(residual.getTargetDate(), LocalTime.of(residual.getHour(), 0));

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

    /**
     * 섭취 시간을 기준으로 24시간 동안의 카페인 잔존량을 계산하고, 기존 잔존량 데이터를 갱신하거나 새로 생성하여 저장합니다.
     *
     * @param user                사용자 정보 객체
     * @param intakeTime          섭취 시간
     * @param initialCaffeineAmount 섭취 시점의 총 카페인 양 (mg)
     */
    private void updateResidualAmounts(User user, LocalDateTime intakeTime, float initialCaffeineAmount) {
        LocalDateTime endTime = intakeTime.plusHours(24);

        List<CaffeineResidual> residuals = new ArrayList<>();
        for (int hourOffset = 0; hourOffset <= 24; hourOffset++) {
            LocalDateTime targetTime = intakeTime.plusHours(hourOffset);
            LocalDate targetDate = targetTime.toLocalDate();
            int hour = targetTime.getHour();
            double timeElapsed = hourOffset;
            double residualAmount = initialCaffeineAmount * Math.exp(-k * timeElapsed);

            Optional<CaffeineResidual> existingResidual = residualRepository.findByUserAndTargetDateAndHour(user, targetDate, hour);
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
                    .targetDate(targetDate)
                    .hour(hour)
                    .residueAmountMg((float) residualAmount)
                    .build();
                residuals.add(residual);
            }
        }
        residualRepository.saveAll(residuals);
    }

    public void deleteCaffeineIntake(Long intakeId) {
        // 1. 수정할 섭취 기록 조회
        CaffeineIntake intake = getCaffeineIntakeById(intakeId);
        User user = intake.getUser();
        LocalDateTime previousIntakeTime = intake.getIntakeTime();
        float previousCaffeineAmount = intake.getCaffeineAmountMg();
        int previousDrinkCount = intake.getDrinkCount();

        // 2. 해당 섭취 내역의 영향이 있는 시간 범위 내의 카페인 잔존량 수치 수정
        modifyResidualAmounts(user, previousIntakeTime, previousCaffeineAmount * previousDrinkCount);

        // 3. 해당 데이터 CaffeineIntakes 테이블에서 삭제
        intakeRepository.deleteById(intakeId);
    }
}