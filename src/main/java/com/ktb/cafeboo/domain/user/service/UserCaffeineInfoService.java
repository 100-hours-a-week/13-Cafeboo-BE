package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.drink.model.DrinkType;
import com.ktb.cafeboo.domain.drink.repository.DrinkTypeRepository;
import com.ktb.cafeboo.domain.recommend.service.CaffeineRecommendationService;
import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.mapper.UserCaffeineInfoMapper;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.model.UserFavoriteDrinkType;
import com.ktb.cafeboo.domain.user.repository.UserCaffeineInfoRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
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

    @Transactional
    public UserCaffeineInfoCreateResponse create(Long userId, UserCaffeineInfoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (userCaffeineInfoRepository.existsByUserId(userId)) {
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserCaffeinInfo entity = UserCaffeineInfoMapper.toEntity(request, user);

            entity.setSleepSensitiveThresholdMg(100f);  // 기본값

            // AI 서버 호출로 하루 최대 카페인 허용량 예측
            try {
                float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user);
                entity.setDailyCaffeineLimitMg(predictedLimit);
            } catch (Exception e) {
                log.warn("[AI 서버 호출 실패] 기존 카페인 허용량으로 설정합니다. userId: {}", userId);
                entity.setDailyCaffeineLimitMg(400f);  // 기본값
            }

            List<UserFavoriteDrinkType> favoriteDrinkTypes = Optional.ofNullable(request.getUserFavoriteDrinks())
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

            return UserCaffeineInfoCreateResponse.builder()
                    .userId(user.getId())
                    .createdAt(entity.getCreatedAt())
                    .build();

        } catch (Exception e) {
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public UserCaffeineInfoUpdateResponse update(Long userId, UserCaffeineInfoUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserCaffeinInfo entity = userCaffeineInfoRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_NOT_FOUND));

        try {
            UserCaffeineInfoMapper.updateEntity(entity, request);

            // AI 서버 호출로 하루 최대 카페인 허용량 예측
            try {
                float predictedLimit = caffeineRecommendationService.getPredictedCaffeineLimitByRule(user);
                entity.setDailyCaffeineLimitMg(predictedLimit);
            } catch (Exception e) {
                log.warn("[AI 서버 호출 실패] 기존 최대 허용 카페인량 유지. userId: {}", userId);
                // 값 유지: set 하지 않음
            }

            List<UserFavoriteDrinkType> favoriteDrinkTypes = Optional.ofNullable(request.getUserFavoriteDrinks())
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

            return UserCaffeineInfoUpdateResponse.builder()
                    .userId(user.getId())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public UserCaffeineInfoResponse getCaffeineInfo(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        UserCaffeinInfo entity = userCaffeineInfoRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_NOT_FOUND));

        return UserCaffeineInfoMapper.toResponse(entity);
    }
}
