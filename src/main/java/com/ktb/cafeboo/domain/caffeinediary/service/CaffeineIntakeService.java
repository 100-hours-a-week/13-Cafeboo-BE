package com.ktb.cafeboo.domain.caffeinediary.service;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineResidualRepository;
import com.ktb.cafeboo.domain.drink.service.DrinkService;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class CaffeineIntakeService {

    private final CaffeineIntakeRepository intakeRepository;
    private final CaffeineResidualRepository residualRepository;

    private final DailyStatisticsService dailyStatisticsService;
    private final UserService userService;
    private final DrinkService drinkService;
    private final CaffeineResidualService caffeineResidualService;

    private static final double DEFAULT_HALF_LIFE_HOUR = 5.0; // 평균 반감기

    // 카페인 반감기 (현재는 임의로 5시간으로 설정)
    private double k = Math.log(2) / DEFAULT_HALF_LIFE_HOUR;

    /**
     * 새로운 카페인 섭취 기록을 등록하고, 섭취 시간을 기준으로 사용자의 예상 카페인 잔존량을 계산하여 데이터베이스에 업데이트합니다.
     *
     * @param userId    사용자 PK, 고유키
     * @param request 요청으로 보낸 drink 정보
     * @return CaffeineIntakeResponse: 생성된 카페인 섭취 기록에 대한 응답
     * @throws IllegalArgumentException 유저 정보 또는 음료 정보가 없을 경우 발생합니다.
     */
    public CaffeineIntakeResponse recordCaffeineIntake(Long userId, CaffeineIntakeRequest request) {
        // 0. 데이터 유효성 겁사. 사용자 정보, 음료 정보가 유효하지 않을 시 IllegalArgumentException 발생
        User user = userService.findUserById(userId);
        Drink drink = drinkService.findDrinkById(request.getDrinkId());

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
        caffeineResidualService.updateResidualAmounts(user, request.getIntakeTime(), request.getCaffeineAmount());

        // 3. DailyStatistics 업데이트
        dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(request.getIntakeTime()), request.getCaffeineAmount());

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
            Drink drink = drinkService.findDrinkById(request.getDrinkId());
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
        caffeineResidualService.modifyResidualAmounts(user, previousIntakeTime, previousCaffeineAmount);
        dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(previousIntakeTime), previousCaffeineAmount * -1);

        // 새로운 시간 기준으로 update -> 수정 후의 잔존량 계산 및 저장
        caffeineResidualService.updateResidualAmounts(user, newIntakeTime, newCaffeineAmount);
        dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(request.getIntakeTime()), request.getCaffeineAmount());

        return CaffeineIntakeResponse.builder()
            .id(intake.getId())
            .drinkId(intake.getDrink().getId())
            .drinkName(intake.getDrink().getName())
            .intakeTime(intake.getIntakeTime())
            .drinkCount(intake.getDrinkCount())
            .caffeineAmount(intake.getCaffeineAmountMg())
            .build();
    }

    public void deleteCaffeineIntake(Long intakeId) {
        // 1. 수정할 섭취 기록 조회
        CaffeineIntake intake = getCaffeineIntakeById(intakeId);
        User user = intake.getUser();
        LocalDateTime previousIntakeTime = intake.getIntakeTime();
        float previousCaffeineAmount = intake.getCaffeineAmountMg();
        int previousDrinkCount = intake.getDrinkCount();

        // 2. 해당 섭취 내역의 영향이 있는 시간 범위 내의 카페인 잔존량 수치 수정
        caffeineResidualService.modifyResidualAmounts(user, previousIntakeTime, previousCaffeineAmount);
        dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(previousIntakeTime), previousCaffeineAmount * -1);

        // 3. 해당 데이터 CaffeineIntakes 테이블에서 삭제
        intakeRepository.deleteById(intakeId);
    }
}