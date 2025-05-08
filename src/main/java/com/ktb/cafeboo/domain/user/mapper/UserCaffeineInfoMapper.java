package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoResponse;
import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoUpdateRequest;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;

import java.time.LocalTime;

public class UserCaffeineInfoMapper {

    public static UserCaffeinInfo toEntity(UserCaffeineInfoCreateRequest dto, User user) {

        return UserCaffeinInfo.builder()
                .user(user)
                .caffeineSensitivity(dto.getCaffeineSensitivity())
                .averageDailyCaffeineIntake(dto.getAverageDailyCaffeineIntake())
                .frequentDrinkTime(LocalTime.parse(dto.getFrequentDrinkTime()))
                // TODO: 인공지능 서버로 하루 임계치 계산 필요
                .dailyCaffeineLimitMg(400)
                .sleepSensitiveThresholdMg(100)
                .build();
    }

    public static void updateEntity(UserCaffeinInfo entity, UserCaffeineInfoUpdateRequest dto) {
        if (dto.getCaffeineSensitivity() != null)
            entity.setCaffeineSensitivity(dto.getCaffeineSensitivity());
        if (dto.getAverageDailyCaffeineIntake() != null)
            entity.setAverageDailyCaffeineIntake(dto.getAverageDailyCaffeineIntake());
        if (dto.getFrequentDrinkTime() != null)
            entity.setFrequentDrinkTime(LocalTime.parse(dto.getFrequentDrinkTime()));
    }

    public static UserCaffeineInfoResponse toResponse(UserCaffeinInfo entity) {
        return UserCaffeineInfoResponse.builder()
                .caffeineSensitivity(entity.getCaffeineSensitivity())
                .averageDailyCaffeineIntake(entity.getAverageDailyCaffeineIntake())
                .frequentDrinkTime(entity.getFrequentDrinkTime().toString())
                .dailyCaffeineLimitMg(entity.getDailyCaffeineLimitMg())
                .sleepSensitiveThresholdMg(entity.getSleepSensitiveThresholdMg())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}