package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateRequest;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;

import java.time.LocalTime;

public class UserHealthInfoMapper {

    public static UserHealthInfo toEntity(UserHealthInfoCreateRequest dto, User user) {

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
}
