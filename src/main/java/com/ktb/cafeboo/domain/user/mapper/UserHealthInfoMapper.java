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
        if (dto.gender() != null) entity.setGender(dto.gender());
        if (dto.age() != null) entity.setAge(dto.age());
        if (dto.height() != null) entity.setHeight(dto.height());
        if (dto.weight() != null) entity.setWeight(dto.weight());
        if (dto.pregnant() != null) entity.setPregnant(dto.pregnant());
        if (dto.takingBirthPill() != null) entity.setTakingBirthPill(dto.takingBirthPill());
        if (dto.smoking() != null) entity.setSmoking(dto.smoking());
        if (dto.hasLiverDisease() != null) entity.setHasLiverDisease(dto.hasLiverDisease());
        if (dto.sleepTime() != null) entity.setSleepTime(LocalTime.parse(dto.sleepTime()));
        if (dto.wakeUpTime() != null) entity.setWakeUpTime(LocalTime.parse(dto.wakeUpTime()));
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