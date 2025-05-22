package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.drink.model.DrinkType;
import com.ktb.cafeboo.domain.drink.repository.DrinkTypeRepository;
import com.ktb.cafeboo.domain.ai.service.CaffeineRecommendationService;
import com.ktb.cafeboo.domain.report.service.DailyStatisticsService;
import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.mapper.UserCaffeineInfoMapper;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.model.UserFavoriteDrinkType;
import com.ktb.cafeboo.domain.user.repository.UserCaffeineInfoRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCaffeineInfoService {

    private final UserRepository userRepository;
    private final UserCaffeineInfoRepository userCaffeineInfoRepository;
    private final DrinkTypeRepository drinkTypeRepository;
    private final CaffeineRecommendationService caffeineRecommendationService;
    private final DailyStatisticsService dailyStatisticsService;

    @Transactional
    public UserCaffeineInfoCreateResponse create(Long userId, UserCaffeineInfoCreateRequest request) {
        log.info("[UserCaffeineInfoService.create] 카페인 정보 생성 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserCaffeineInfoService.create] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        if (userCaffeineInfoRepository.existsByUserId(userId)) {
            log.warn("[UserCaffeineInfoService.create] 카페인 정보 이미 존재 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserCaffeinInfo entity = UserCaffeineInfoMapper.toEntity(request, user);
            entity.setSleepSensitiveThresholdMg(100f);  // 기본값

            try {
                float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user, entity.getCaffeineSensitivity());
                entity.setDailyCaffeineLimitMg(predictedLimit);
                log.info("[UserCaffeineInfoService.create] AI 기반 최대 카페인 허용량 예측 성공 - userId={}", userId);
            } catch (Exception e) {
                log.warn("[UserCaffeineInfoService.create] AI 서버 호출 실패 - 기본 허용량 적용 - userId={}", userId);
                entity.setDailyCaffeineLimitMg(400f);  // 기본값
            }

            List<UserFavoriteDrinkType> favoriteDrinkTypes = Optional.ofNullable(request.userFavoriteDrinks())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(StringUtils::hasText)
                    .map(drinkName -> {
                        DrinkType drinkType = drinkTypeRepository.findByName(drinkName)
                                .orElseGet(() -> drinkTypeRepository.save(new DrinkType(drinkName)));
                        UserFavoriteDrinkType favorite = new UserFavoriteDrinkType();
                        favorite.setUser(user);
                        favorite.setDrinkType(drinkType);
                        return favorite;
                    }).toList();

            user.setFavoriteDrinks(favoriteDrinkTypes);
            userCaffeineInfoRepository.save(entity);

            log.info("[UserCaffeineInfoService.create] 카페인 정보 생성 완료 - userId={}", userId);

            return new UserCaffeineInfoCreateResponse(
                    user.getId().toString(),
                    entity.getCreatedAt()
            );

        } catch (Exception e) {
            log.error("[UserCaffeineInfoService.create] 카페인 정보 생성 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserCaffeineInfoUpdateResponse update(Long userId, UserCaffeineInfoUpdateRequest request) {
        log.info("[UserCaffeineInfoService.update] 카페인 정보 수정 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserCaffeineInfoService.update] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserCaffeinInfo entity = user.getCaffeinInfo();
        if (entity == null) {
            log.warn("[UserCaffeineInfoService.update] 카페인 정보 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_NOT_FOUND);
        }

        try {
            UserCaffeineInfoMapper.updateEntity(entity, request);

            try {
                float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user, entity.getCaffeineSensitivity());
                entity.setDailyCaffeineLimitMg(predictedLimit);
                dailyStatisticsService.updateDailyStatisticsAfterUpdateUserInfo(user, LocalDate.now());
                log.info("[UserCaffeineInfoService.update] AI 기반 허용량 갱신 및 통계 반영 완료 - userId={}", userId);
            } catch (Exception e) {
                log.warn("[UserCaffeineInfoService.update] AI 서버 호출 실패 - 기존 허용량 유지 - userId={}", userId);
            }

            List<UserFavoriteDrinkType> favoriteDrinkTypes = Optional.ofNullable(request.userFavoriteDrinks())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(StringUtils::hasText)
                    .map(drinkName -> {
                        DrinkType drinkType = drinkTypeRepository.findByName(drinkName)
                                .orElseGet(() -> drinkTypeRepository.save(new DrinkType(drinkName)));
                        UserFavoriteDrinkType favorite = new UserFavoriteDrinkType();
                        favorite.setUser(user);
                        favorite.setDrinkType(drinkType);
                        return favorite;
                    }).toList();

            if (!favoriteDrinkTypes.isEmpty()) {
                user.setFavoriteDrinks(favoriteDrinkTypes);
            }

            log.info("[UserCaffeineInfoService.update] 카페인 정보 수정 완료 - userId={}", userId);

            return new UserCaffeineInfoUpdateResponse(
                    user.getId().toString(),
                    entity.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("[UserCaffeineInfoService.update] 카페인 정보 수정 실패 - userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public UserCaffeineInfoResponse getCaffeineInfo(Long userId) {
        log.info("[UserCaffeineInfoService.getCaffeineInfo] 카페인 정보 조회 요청 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserCaffeineInfoService.getCaffeineInfo] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        UserCaffeinInfo entity = user.getCaffeinInfo();
        if (entity == null) {
            log.warn("[UserCaffeineInfoService.getCaffeineInfo] 카페인 정보 없음 - userId={}", userId);
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_NOT_FOUND);
        }

        log.info("[UserCaffeineInfoService.getCaffeineInfo] 카페인 정보 조회 성공 - userId={}", userId);
        return UserCaffeineInfoMapper.toResponse(entity);
    }
}