package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.ai.service.CaffeineRecommendationService;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.mapper.UserHealthInfoMapper;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.repository.UserHealthInfoRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHealthInfoService {

    private final UserRepository userRepository;
    private final UserHealthInfoRepository userHealthInfoRepository;
    private final CaffeineRecommendationService caffeineRecommendationService;
    private final DailyStatisticsService dailyStatisticsService;

    @Transactional
    public UserHealthInfoCreateResponse create(Long userId, UserHealthInfoCreateRequest request) {
        log.info("[UserHealthInfoService.create] 건강 정보 생성 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserHealthInfoService.create] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        if (userHealthInfoRepository.existsByUserId(userId)) {
            log.warn("[UserHealthInfoService.create] 건강 정보 이미 존재 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserHealthInfo entity = UserHealthInfoMapper.toEntity(request, user);
            userHealthInfoRepository.save(entity);

            log.info("[UserHealthInfoService.create] 건강 정보 생성 완료 - userId={}", userId);
            return new UserHealthInfoCreateResponse(
                    user.getId().toString(),
                    entity.getCreatedAt()
            );
        } catch (Exception e) {
            log.error("[UserHealthInfoService.create] 건강 정보 생성 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserHealthInfoUpdateResponse update(Long userId, UserHealthInfoUpdateRequest request) {
        log.info("[UserHealthInfoService.update] 건강 정보 수정 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserHealthInfoService.update] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            log.warn("[UserHealthInfoService.update] 건강 정보 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        try {
            UserHealthInfoMapper.updateEntity(healthInfo, request);

            try {
                UserCaffeinInfo caffeinInfo = user.getCaffeinInfo();
                if (caffeinInfo != null) {
                    float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user, caffeinInfo.getCaffeineSensitivity());
                    caffeinInfo.setDailyCaffeineLimitMg(predictedLimit);
                    dailyStatisticsService.updateDailyStatisticsAfterUpdateUserInfo(user, LocalDate.now());
                    log.info("[UserHealthInfoService.update] AI 기반 허용량 갱신 및 통계 반영 완료 - userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("[UserHealthInfoService.update] AI 서버 호출 실패 - 기존 허용량 유지 - userId={}", userId);
            }

            log.info("[UserHealthInfoService.update] 건강 정보 수정 완료 - userId={}", userId);
            return new UserHealthInfoUpdateResponse(
                    userId.toString(),
                    healthInfo.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("[UserHealthInfoService.update] 건강 정보 수정 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public UserHealthInfoResponse getHealthInfo(Long userId) {
        log.info("[UserHealthInfoService.getHealthInfo] 건강 정보 조회 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserHealthInfoService.getHealthInfo] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            log.warn("[UserHealthInfoService.getHealthInfo] 건강 정보 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        log.info("[UserHealthInfoService.getHealthInfo] 건강 정보 조회 성공 - userId={}", userId);
        return UserHealthInfoMapper.toResponse(healthInfo);
    }
}
