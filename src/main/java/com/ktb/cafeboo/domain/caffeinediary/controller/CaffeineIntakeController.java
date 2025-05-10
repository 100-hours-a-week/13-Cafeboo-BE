package com.ktb.cafeboo.domain.caffeinediary.controller;

import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeRequest;
import com.ktb.cafeboo.domain.caffeinediary.dto.CaffeineIntakeResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.DailyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.dto.MonthlyCaffeineDiaryResponse;
import com.ktb.cafeboo.domain.caffeinediary.model.CaffeineIntake;
import com.ktb.cafeboo.domain.caffeinediary.service.CaffeineIntakeService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.service.UserService;
import com.ktb.cafeboo.global.apiPayload.ApiResponse;
import com.ktb.cafeboo.global.apiPayload.code.status.SuccessStatus;
import com.ktb.cafeboo.global.security.userdetails.CustomUserDetails;
import com.ktb.cafeboo.global.util.AuthChecker;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caffeine-intakes")
@RequiredArgsConstructor
@Slf4j
public class CaffeineIntakeController {
    private final CaffeineIntakeService caffeineIntakeService;

    @PostMapping
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> recordCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CaffeineIntakeRequest request) {

        Long userId = userDetails.getId();

        // 1. 서비스 메서드 호출
        CaffeineIntakeResponse response = caffeineIntakeService.recordCaffeineIntake(userId, request);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_RECORDED, response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>> updateCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id,
        @RequestBody CaffeineIntakeRequest request) {

        // 1. 서비스 메서드 호출
        Long userId = userDetails.getId();
        CaffeineIntakeResponse response = caffeineIntakeService.updateCaffeineIntake(id, request);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_UPDATED, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CaffeineIntakeResponse>>deleteCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long id) {

        // 1. 서비스 메서드 호출
        Long userId = userDetails.getId();
        caffeineIntakeService.deleteCaffeineIntake(id);

        // 2. 응답 반환
        return ResponseEntity.ok(ApiResponse.of(SuccessStatus.CAFFEINE_INTAKE_DELETED, null));
    }

    @GetMapping
    @RequestMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyCaffeineDiaryResponse>>getCaffeineIntakeDiary(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("year") String targetYear,
        @RequestParam("month") String targetMonth){

        Long userId = userDetails.getId();
        int year = Integer.parseInt(targetYear);
        int month = Integer.parseInt(targetMonth);

        List<CaffeineIntake> intakes = caffeineIntakeService.getCaffeineIntakeDiary(userId, year, month);
        List<MonthlyCaffeineDiaryResponse.DailyIntake> dailyIntakeList =
            caffeineIntakeService.getDailyIntakeListForMonth(intakes, year, month);

        MonthlyCaffeineDiaryResponse response = MonthlyCaffeineDiaryResponse.builder()
            .filter(MonthlyCaffeineDiaryResponse.Filter.builder()
                .year(String.valueOf(year))
                .month(String.valueOf(month))
                .build())
            .dailyIntakeList(dailyIntakeList)
            .build();

        return ResponseEntity.ok(ApiResponse.of(
            SuccessStatus.MONTHLY_CAFFEINE_CALENDAR_SUCCESS, response));
    }

    @GetMapping
    @RequestMapping("/daily")
    public ResponseEntity<ApiResponse<DailyCaffeineDiaryResponse>> getDailyCaffeineIntake(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam("date") String date) {

        Long userId = userDetails.getId();
        LocalDate localDate = LocalDate.parse(date);
        List<CaffeineIntake> intakes = caffeineIntakeService.getDailyCaffeineIntake(userId, localDate);

        // 총 카페인 섭취량 계산
        float totalCaffeineMg = (float) intakes.stream()
            .mapToDouble(CaffeineIntake::getCaffeineAmountMg)
            .sum();

        // intakeList 생성
        List<DailyCaffeineDiaryResponse.IntakeDetail> intakeList = intakes.stream()
            .map(intake -> DailyCaffeineDiaryResponse.IntakeDetail.builder()
                .intakeId(intake.getId())
                .drinkName(intake.getDrink().getName())
                .drinkCount(intake.getDrinkCount())
                .caffeineMg(intake.getCaffeineAmountMg())
                .intakeTime(intake.getIntakeTime().toString()) // ISO 8601
                .build())
            .collect(Collectors.toList());

        DailyCaffeineDiaryResponse response = DailyCaffeineDiaryResponse.builder()
            .filter(DailyCaffeineDiaryResponse.Filter.builder()
                .date(date.toString())
                .build())
            .totalCaffeineMg(totalCaffeineMg)
            .intakeList(intakeList)
            .build();

        return ResponseEntity.ok(ApiResponse.of(
            SuccessStatus.DAILY_CAFFEINE_CALENDAR_SUCCESS, response));
    }
}
