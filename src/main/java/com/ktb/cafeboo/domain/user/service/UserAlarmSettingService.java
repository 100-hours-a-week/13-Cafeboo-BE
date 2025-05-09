package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserAlarmSetting;
import com.ktb.cafeboo.domain.user.repository.UserAlarmSettingRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ktb.cafeboo.domain.user.mapper.UserAlarmSettingMapper;

@Service
@RequiredArgsConstructor
public class UserAlarmSettingService {

    private final UserRepository userRepository;
    private final UserAlarmSettingRepository userAlarmSettingRepository;

    @Transactional
    public UserAlarmSettingCreateResponse create(Long userId, UserAlarmSettingCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (userAlarmSettingRepository.existsByUserId(userId)) {
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_ALREADY_EXISTS);
        }

        try {
            UserAlarmSetting entity = UserAlarmSettingMapper.toEntity(request, user);
            userAlarmSettingRepository.save(entity);
            return UserAlarmSettingCreateResponse.builder()
                    .userId(user.getId().toString())
                    .createdAt(entity.getCreatedAt())
                    .build();
        } catch (Exception e) {
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserAlarmSettingUpdateResponse update(Long userId, UserAlarmSettingUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserAlarmSetting entity = user.getAlarmSetting();
        if (entity == null) {
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_NOT_FOUND);
        }

        try {
            UserAlarmSettingMapper.updateEntity(entity, request);
            return UserAlarmSettingUpdateResponse.builder()
                    .userId(user.getId().toString())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public UserAlarmSettingResponse get(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserAlarmSetting entity = user.getAlarmSetting();
        if (entity == null) {
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_NOT_FOUND);
        }

        return UserAlarmSettingMapper.toResponse(entity);
    }
}
