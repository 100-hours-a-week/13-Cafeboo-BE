package com.ktb.cafeboo.domain.caffeinediary.service;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.DailyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.drink.model.Drink;
import com.ktb.cafeboo.domain.caffeinediary.repository.CaffeineIntakeRepository;
import com.ktb.cafeboo.domain.drink.model.DrinkSizeNutrition;
import com.ktb.cafeboo.domain.drink.service.DrinkService;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.DrinkSize;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CaffeineIntakeService {

    private final CaffeineIntakeRepository intakeRepository;

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
        log.info("[recordCaffeineIntake] 호출됨 - userId={}, request={}", userId, request);

        // 0. 사용자 정보 조회
        User user;
        try {
            user = userService.findUserById(userId);
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 사용자 정보 조회 실패", e);
            throw e;
        }

        // 1. request validation
        if (request.drinkId() == null || request.drinkSize() == null || request.intakeTime() == null ||
                request.drinkCount() == null || request.caffeineAmount() == null ||
                request.drinkId().isEmpty() || request.drinkSize().isEmpty()) {
            log.error("[recordCaffeineIntake] 필수 필드 누락 - request={}", request);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        // 2. 음료 조회
        Drink drink;
        try {
            drink = drinkService.findDrinkById(Long.parseLong(request.drinkId()));
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 음료 조회 실패", e);
            throw e;
        }

        // 3. 음료 사이즈 및 영양 정보 조회
        DrinkSizeNutrition drinkSizeNutrition;
        try {
            DrinkSize drinkSize = DrinkSize.valueOf(request.drinkSize());
            drinkSizeNutrition = drinkService.findDrinkSizeNutritionByIdAndSize(drink.getId(), drinkSize);
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 사이즈/영양 정보 조회 실패", e);
            throw e;
        }

        // 4. 섭취 정보 저장
        CaffeineIntake intake;
        try {
            intake = CaffeineIntake.builder()
                    .user(user)
                    .drink(drink)
                    .drinkSizeNutrition(drinkSizeNutrition)
                    .intakeTime(request.intakeTime())
                    .drinkCount(request.drinkCount())
                    .caffeineAmountMg(request.caffeineAmount())
                    .build();
            intakeRepository.save(intake);
            log.info("[recordCaffeineIntake] 섭취 정보 저장 완료 - intakeId={}", intake.getId());
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 섭취 정보 저장 실패", e);
            throw e;
        }

        // 5. 잔존량 계산
        try {
            caffeineResidualService.updateResidualAmounts(userId, request.intakeTime(), request.caffeineAmount());
            log.info("[recordCaffeineIntake] 잔존량 계산 완료");
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 잔존량 계산 실패", e);
            throw e;
        }

        // 6. 일일 통계 업데이트
        try {
            dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(request.intakeTime()), request.caffeineAmount());
            log.info("[recordCaffeineIntake] 일일 통계 업데이트 완료");
        } catch (Exception e) {
            log.error("[recordCaffeineIntake] 일일 통계 업데이트 실패", e);
            throw e;
        }

        // 7. 응답 생성
        return new CaffeineIntakeResponse(
                intake.getId().toString(),
                request.drinkId(),
                drink.getName(),
                request.intakeTime(),
                request.drinkCount(),
                request.caffeineAmount()
        );
    }

    /**
     * 카페인 섭취 기록 ID로 특정 섭취 기록을 조회합니다.
     * @param intakeId 조회할 카페인 섭취 기록 ID
     * @return 조회된 카페인 섭취 기록
     * @throws IllegalArgumentException 해당 ID의 섭취 기록이 없을 경우 발생
     */
    public CaffeineIntake getCaffeineIntakeById(Long intakeId) {
        return intakeRepository.findById(intakeId)
            .orElseThrow(() -> new CustomApiException(ErrorStatus.INTAKE_NOT_FOUND));
    }

    /**
     * 카페인 섭취 기록을 수정합니다. 요청에 포함된 필드만 수정합니다.
     * @param intakeId 수정할 카페인 섭취 기록 ID
     * @param request 수정할 내용을 담은 DTO
     * @return 수정된 카페인 섭취 기록에 대한 응답
     * @throws IllegalArgumentException 해당 ID의 섭취 기록이 없거나, 유효하지 않은 음료 ID가 입력된 경우 발생
     */
    public CaffeineIntakeResponse updateCaffeineIntake(Long intakeId, CaffeineIntakeRequest request)
        throws Exception {
        // 1. 수정할 섭취 기록 조회
        try {
            CaffeineIntake intake = getCaffeineIntakeById(intakeId);

            User user = intake.getUser();
            LocalDateTime previousIntakeTime = intake.getIntakeTime();
            float previousCaffeineAmount = intake.getCaffeineAmountMg();
            int previousDrinkCount = intake.getDrinkCount();

            // 2. 수정할 필드 적용
            if (request.drinkId() != null) {
                Drink drink = drinkService.findDrinkById(Long.parseLong(request.drinkId()));
                intake.setDrink(drink);
            }
            if (request.intakeTime() != null) {
                intake.setIntakeTime(request.intakeTime());
            }
            if (request.drinkCount() != null) {
                intake.setDrinkCount(request.drinkCount());
            }
            if (request.caffeineAmount() != null) {
                intake.setCaffeineAmountMg(request.caffeineAmount());
            }
            if (request.drinkSize() != null) {
                DrinkSize drinkSize = DrinkSize.valueOf(request.drinkSize());

                DrinkSizeNutrition drinkSizeNutrition = request.drinkId() != null
                        ? drinkService.findDrinkSizeNutritionByIdAndSize(Long.parseLong(request.drinkId()), drinkSize)
                        : drinkService.findDrinkSizeNutritionByIdAndSize(intake.getDrink().getId(), drinkSize);

                intake.setDrinkSizeNutrition(drinkSizeNutrition);
            }

            // 3. 연관된 잔존량 정보 수정. 수정된 섭취 시간, 음료 잔 수에 따라 잔존량 정보를 다시 계산해야 함
            float newCaffeineAmount =
                    request.caffeineAmount() != null ? request.caffeineAmount() : intake.getCaffeineAmountMg();

            int newDrinkCount =
                    request.drinkCount() != null ? request.drinkCount() : intake.getDrinkCount();

            LocalDateTime newIntakeTime =
                    request.intakeTime() != null ? request.intakeTime() : intake.getIntakeTime();

            // 기존 시간 기준 삭제 ->  수정 이전의 잔존량 삭제
            caffeineResidualService.modifyResidualAmounts(user.getId(), previousIntakeTime,
                previousCaffeineAmount);
            dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(previousIntakeTime),
                previousCaffeineAmount * -1);

            // 새로운 시간 기준으로 update -> 수정 후의 잔존량 계산 및 저장
            caffeineResidualService.updateResidualAmounts(user.getId(), newIntakeTime,
                newCaffeineAmount);
            dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(newIntakeTime),
                newCaffeineAmount);

            intakeRepository.save(intake);

            return new CaffeineIntakeResponse(
                    intakeId.toString(),
                    intake.getDrink().getId().toString(),
                    intake.getDrink().getName(),
                    newIntakeTime,
                    newDrinkCount,
                    newCaffeineAmount
            );

        }
        catch (CustomApiException e){
            throw new CustomApiException(ErrorStatus.INTAKE_NOT_FOUND);
        }
        catch(Exception e){ // Error 대신 Exception으로 catch 하는 것이 더 일반적입니다.
            log.error("[CaffeineIntakeService.updateCaffeineIntake] 섭취 기록 수정 실패 - intakeId: {}, request: {}", intakeId, request);
            throw new CustomApiException(ErrorStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteCaffeineIntake(Long intakeId) {
        try{
            // 1. 수정할 섭취 기록 조회
            CaffeineIntake intake = getCaffeineIntakeById(intakeId);
            User user = intake.getUser();
            LocalDateTime previousIntakeTime = intake.getIntakeTime();
            float previousCaffeineAmount = intake.getCaffeineAmountMg();
            int previousDrinkCount = intake.getDrinkCount();

            // 2. 해당 섭취 내역의 영향이 있는 시간 범위 내의 카페인 잔존량 수치 수정
            caffeineResidualService.modifyResidualAmounts(user.getId(), previousIntakeTime, previousCaffeineAmount);
            dailyStatisticsService.updateDailyStatistics(user, LocalDate.from(previousIntakeTime), previousCaffeineAmount * -1);

            // 3. 해당 데이터 CaffeineIntakes 테이블에서 삭제
            intakeRepository.deleteById(intakeId);
        }
        catch(Exception e){
            log.error("[CaffeineIntakeService.deleteCaffeineIntake] 섭취 기록 삭제 실패 - intakeId: {}", intakeId);
            throw new CustomApiException(ErrorStatus.INTAKE_DELETE_FAILED);
        }
    }

    public MonthlyCaffeineDiaryResponse getCaffeineIntakeDiary(Long userId, String targetYear, String targetMonth){
        if(targetYear == null || targetMonth == null || targetYear.isEmpty() || targetMonth.isEmpty()){
            log.error("[CaffeineIntakeService.getCaffeineIntakeDiary] 파라미터 누락 - year={}, month={}", targetYear, targetMonth);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        int year, month;

        try{
            year = Integer.parseInt(targetYear);
            month = Integer.parseInt(targetMonth);
        }
        catch (Exception e){
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1);
        List<CaffeineIntake> intakes =  intakeRepository.findByUserIdAndIntakeTimeBetween(userId, start, end);

        List<MonthlyCaffeineDiaryResponse.DailyIntake> dailyIntakeList =
            getDailyIntakeListForMonth(intakes, targetYear, targetMonth);

        return new MonthlyCaffeineDiaryResponse(
                new MonthlyCaffeineDiaryResponse.Filter(targetYear, String.valueOf(targetMonth)),
                dailyIntakeList
        );
    }

    public List<MonthlyCaffeineDiaryResponse.DailyIntake> getDailyIntakeListForMonth(List<CaffeineIntake> intakes, String targetYear, String targetMonth) {
        // 1. 일별 합계 Map 생성
        if(targetYear == null || targetMonth == null || targetYear.isEmpty() || targetMonth.isEmpty()){
            log.error("[CaffeineIntakeService.getDailyIntakeListForMonth] 파라미터 누락 - year={}, month={}", targetYear, targetMonth);
            throw new CustomApiException(ErrorStatus.INVALID_PARAMETER);
        }

        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);
        Map<LocalDate, Float> dailySumMap = intakes.stream()
            .collect(Collectors.groupingBy(
                intake -> intake.getIntakeTime().toLocalDate(),
                Collectors.summingDouble(CaffeineIntake::getCaffeineAmountMg)
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().floatValue()));

        // 2. 월의 모든 날짜에 대해 리스트 생성 (없는 날은 0)
        YearMonth yearMonth = YearMonth.of(year, month);
        List<MonthlyCaffeineDiaryResponse.DailyIntake> dailyIntakeList = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            float total = dailySumMap.getOrDefault(date, 0f);
            dailyIntakeList.add(
              new MonthlyCaffeineDiaryResponse.DailyIntake(date.toString(), total)
            );
        }
        return dailyIntakeList;
    }

    public DailyCaffeineDiaryResponse getDailyCaffeineIntake(Long userId, String targetDate){
        if(targetDate == null || targetDate.isEmpty()){
            log.error("[CaffeineIntakeService.getDailyCaffeineIntake] 파라미터 누락 - date={}", targetDate);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        LocalDate date;
        try{
            date = LocalDate.parse(targetDate);
        }
        catch (Exception e){
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<CaffeineIntake> intakes = intakeRepository.findByUserIdAndIntakeTimeBetween(userId, start, end);

        // 총 카페인 섭취량 계산
        float totalCaffeineMg = (float) intakes.stream()
            .mapToDouble(CaffeineIntake::getCaffeineAmountMg)
            .sum();

        // intakeList 생성
        List<DailyCaffeineDiaryResponse.IntakeDetail> intakeList = intakes.stream()
                .map(intake -> new DailyCaffeineDiaryResponse.IntakeDetail(
                        intake.getId().toString(),
                        intake.getDrink().getId().toString(),
                        intake.getDrink().getName(),
                        intake.getDrinkCount(),
                        intake.getCaffeineAmountMg(),
                        intake.getIntakeTime().toString() // ISO 8601
                ))
                .collect(Collectors.toList());

        return new DailyCaffeineDiaryResponse(
                new DailyCaffeineDiaryResponse.Filter(targetDate),
                totalCaffeineMg,
                intakeList
        );
    }
}