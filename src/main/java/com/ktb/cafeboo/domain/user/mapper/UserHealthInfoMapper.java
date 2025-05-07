package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoUpdateRequest;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;

import java.time.LocalTime;

public class UserHealthInfoMapper {

    public static UserHealthInfo createEntity(UserHealthInfoCreateRequest dto, User user) {

        UserHealthInfo entity = new UserHealthInfo();
        entity.setUser(user);
        entity.setGender(dto.getGender());
        entity.setAge(dto.getAge());
        entity.setHeight(dto.getHeight());
        entity.setWeight(dto.getWeight());
        entity.setPregnant(dto.getPregnant());
        entity.setTakingBirthPill(dto.getTakingBirthPill());
        entity.setSmoking(dto.getSmoking());
        entity.setHasLiverDisease(dto.getHasLiverDisease());
        entity.setSleepTime(LocalTime.parse(dto.getSleepTime()));
        entity.setWakeUpTime(LocalTime.parse(dto.getWakeUpTime()));

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
}
