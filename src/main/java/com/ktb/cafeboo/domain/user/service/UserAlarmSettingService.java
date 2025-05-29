package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserAlarmSetting;
import com.ktb.cafeboo.domain.user.repository.UserAlarmSettingRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ktb.cafeboo.domain.user.mapper.UserAlarmSettingMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAlarmSettingService {

    private final UserRepository userRepository;
    private final UserAlarmSettingRepository userAlarmSettingRepository;

    @Transactional
    public UserAlarmSettingCreateResponse create(Long userId, UserAlarmSettingCreateRequest request) {
        log.info("[UserAlarmSettingService.create] 알람 설정 생성 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserAlarmSettingService.create] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        if (userAlarmSettingRepository.existsByUserId(userId)) {
            log.warn("[UserAlarmSettingService.create] 알람 설정 이미 존재 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_ALREADY_EXISTS);
        }

        try {
            UserAlarmSetting entity = UserAlarmSettingMapper.toEntity(request, user);
            userAlarmSettingRepository.save(entity);
            log.info("[UserAlarmSettingService.create] 알람 설정 생성 완료 - userId={}", userId);
            return new UserAlarmSettingCreateResponse(
                    user.getId().toString(),
                    entity.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("[UserAlarmSettingService.create] 알람 설정 생성 실패 - userId={}, message={}", userId, e.getMessage());
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserAlarmSettingUpdateResponse update(Long userId, UserAlarmSettingUpdateRequest request) {
        log.info("[UserAlarmSettingService.update] 알람 설정 수정 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserAlarmSettingService.update] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserAlarmSetting entity = user.getAlarmSetting();
        if (entity == null) {
            log.warn("[UserAlarmSettingService.update] 알람 설정 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_NOT_FOUND);
        }

        try {
            UserAlarmSettingMapper.updateEntity(entity, request);
            log.info("[UserAlarmSettingService.update] 알람 설정 수정 완료 - userId={}", userId);
            return new UserAlarmSettingUpdateResponse(
                    user.getId().toString(),
                    entity.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("[UserAlarmSettingService.update] 알람 설정 수정 실패 - userId={}, message={}", userId, e.getMessage());
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public UserAlarmSettingResponse get(Long userId) {
        log.info("[UserAlarmSettingService.get] 알람 설정 조회 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserAlarmSettingService.get] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserAlarmSetting entity = user.getAlarmSetting();
        if (entity == null) {
            log.warn("[UserAlarmSettingService.get] 알람 설정 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.ALARM_SETTING_NOT_FOUND);
        }

        log.info("[UserAlarmSettingService.get] 알람 설정 조회 성공 - userId={}", userId);
        return UserAlarmSettingMapper.toResponse(entity);
    }
}