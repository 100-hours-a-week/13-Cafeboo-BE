package com.ktb.cafeboo.domain.user.mapper;

import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserAlarmSetting;

public class UserAlarmSettingMapper {

    public static UserAlarmSetting toEntity(UserAlarmSettingCreateRequest dto, User user) {
        UserAlarmSetting entity = new UserAlarmSetting();
        entity.setUser(user);
        entity.setAlarmWhenExceedIntake(dto.alarmWhenExceedIntake());
        entity.setAlarmBeforeSleep(dto.alarmBeforeSleep());
        entity.setAlarmForChat(dto.alarmForChat());
        return entity;
    }

    public static void updateEntity(UserAlarmSetting entity, UserAlarmSettingUpdateRequest dto) {
        if (dto.alarmWhenExceedIntake() != null) {
            entity.setAlarmWhenExceedIntake(dto.alarmWhenExceedIntake());
        }
        if (dto.alarmBeforeSleep() != null) {
            entity.setAlarmBeforeSleep(dto.alarmBeforeSleep());
        }
        if (dto.alarmForChat() != null) {
            entity.setAlarmForChat(dto.alarmForChat());
        }
    }


    public static UserAlarmSettingResponse toResponse(UserAlarmSetting entity) {
        return new UserAlarmSettingResponse(
            entity.isAlarmWhenExceedIntake(),
            entity.isAlarmBeforeSleep(),
            entity.isAlarmForChat(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
