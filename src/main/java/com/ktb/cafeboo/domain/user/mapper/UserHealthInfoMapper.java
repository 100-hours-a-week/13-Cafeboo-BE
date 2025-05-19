package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoResponse;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoUpdateRequest;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;

import java.time.LocalTime;

public class UserHealthInfoMapper {

    public static UserHealthInfo toEntity(UserHealthInfoCreateRequest dto, User user) {

        UserHealthInfo entity = new UserHealthInfo();
        entity.setUser(user);
        entity.setGender(dto.gender());
        entity.setAge(dto.age());
        entity.setHeight(dto.height());
        entity.setWeight(dto.weight());
        entity.setPregnant(dto.pregnant());
        entity.setTakingBirthPill(dto.takingBirthPill());
        entity.setSmoking(dto.smoking());
        entity.setHasLiverDisease(dto.hasLiverDisease());
        entity.setSleepTime(LocalTime.parse(dto.sleepTime()));
        entity.setWakeUpTime(LocalTime.parse(dto.wakeUpTime()));

        return entity;
    }

    public static void updateEntity(UserHealthInfo entity, UserHealthInfoUpdateRequest dto) {
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getAge() != null) entity.setAge(dto.getAge());
        if (dto.getHeight() != null) entity.setHeight(dto.getHeight());
        if (dto.getWeight() != null) entity.setWeight(dto.getWeight());
        if (dto.getPregnant() != null) entity.setPregnant(dto.getPregnant());
        if (dto.getTakingBirthPill() != null) entity.setTakingBirthPill(dto.getTakingBirthPill());
        if (dto.getSmoking() != null) entity.setSmoking(dto.getSmoking());
        if (dto.getHasLiverDisease() != null) entity.setHasLiverDisease(dto.getHasLiverDisease());
        if (dto.getSleepTime() != null) entity.setSleepTime(LocalTime.parse(dto.getSleepTime()));
        if (dto.getWakeUpTime() != null) entity.setWakeUpTime(LocalTime.parse(dto.getWakeUpTime()));
    }

    public static UserHealthInfoResponse toResponse(UserHealthInfo entity) {
        return new UserHealthInfoResponse(
                entity.getGender(),
                entity.getAge(),
                entity.getHeight(),
                entity.getWeight(),
                entity.getPregnant(),
                entity.getTakingBirthPill(),
                entity.getSmoking(),
                entity.getHasLiverDisease(),
                entity.getSleepTime().toString(),
                entity.getWakeUpTime().toString(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}