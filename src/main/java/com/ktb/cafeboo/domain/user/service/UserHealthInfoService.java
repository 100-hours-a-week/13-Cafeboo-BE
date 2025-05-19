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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserHealthInfoService {

    private final UserRepository userRepository;
    private final UserHealthInfoRepository userHealthInfoRepository;
    private final CaffeineRecommendationService caffeineRecommendationService;
    private final DailyStatisticsService dailyStatisticsService;

    @Transactional
    public UserHealthInfoCreateResponse create(Long userId, UserHealthInfoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (userHealthInfoRepository.existsByUserId(userId)) {
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserHealthInfo entity = UserHealthInfoMapper.toEntity(request, user);
            userHealthInfoRepository.save(entity);

            return UserHealthInfoCreateResponse.builder()
                    .userId(user.getId().toString())
                    .createdAt(entity.getCreatedAt())
                    .build();

        } catch (Exception e) {
            // 매핑/저장 중 오류 발생 시 BAD_REQUEST 반환
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserHealthInfoUpdateResponse update(Long userId, UserHealthInfoUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        try {
            UserHealthInfoMapper.updateEntity(healthInfo, request);

            // 건강 정보 수정 시, 최대 허용 카페인량 업데이트
            try {
                UserCaffeinInfo caffeinInfo = user.getCaffeinInfo();
                if (caffeinInfo != null) {
                    float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user, caffeinInfo.getCaffeineSensitivity());
                    caffeinInfo.setDailyCaffeineLimitMg(predictedLimit);

                    // 유저 카페인 관련 내용 수정 후, 바뀐 카페인 한계치에 따른 내용 일일 통계 데이터에 반영
                    dailyStatisticsService.updateDailyStatisticsAfterUpdateUserInfo(user, LocalDate.now());
                }
            } catch (Exception e) {
                log.warn("[AI 서버 호출 실패] 기존 최대 허용 카페인량 유지. userId: {}", userId);
                // 값 유지
            }

        } catch (Exception e) {
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }

        return UserHealthInfoUpdateResponse.builder()
                .userId(userId.toString())
                .updatedAt(healthInfo.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public UserHealthInfoResponse getHealthInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserHealthInfo healthInfo = user.getHealthInfo();
        if (healthInfo == null) {
            throw new CustomApiException(ErrorStatus.HEALTH_PROFILE_NOT_FOUND);
        }

        return UserHealthInfoMapper.toResponse(healthInfo);
    }
}
