package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoResponse;
import com.ktb.cafeboo.domain.user.dto.UserCaffeineInfoUpdateRequest;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class UserCaffeineInfoMapper {

    public static UserCaffeinInfo toEntity(UserCaffeineInfoCreateRequest dto, User user) {

        return UserCaffeinInfo.builder()
                .user(user)
                .caffeineSensitivity(dto.caffeineSensitivity())
                .averageDailyCaffeineIntake(dto.averageDailyCaffeineIntake())
                .frequentDrinkTime(LocalTime.parse(dto.frequentDrinkTime()))
                .build();
    }

    public static void updateEntity(UserCaffeinInfo entity, UserCaffeineInfoUpdateRequest dto) {
        if (dto.caffeineSensitivity() != null)
            entity.setCaffeineSensitivity(dto.caffeineSensitivity());
        if (dto.averageDailyCaffeineIntake() != null)
            entity.setAverageDailyCaffeineIntake(dto.averageDailyCaffeineIntake());
        if (dto.frequentDrinkTime() != null)
            entity.setFrequentDrinkTime(LocalTime.parse(dto.frequentDrinkTime()));
    }

    public static UserCaffeineInfoResponse toResponse(UserCaffeinInfo entity) {
        List<String> favoriteDrinks = Optional.ofNullable(entity.getUser().getFavoriteDrinks())
                .orElse(List.of())
                .stream()
                .map(fav -> fav.getDrinkType().getName())
                .toList();

        return new UserCaffeineInfoResponse(
                entity.getCaffeineSensitivity(),
                entity.getAverageDailyCaffeineIntake(),
                entity.getFrequentDrinkTime().toString(),
                entity.getDailyCaffeineLimitMg(),
                entity.getSleepSensitiveThresholdMg(),
                favoriteDrinks,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}