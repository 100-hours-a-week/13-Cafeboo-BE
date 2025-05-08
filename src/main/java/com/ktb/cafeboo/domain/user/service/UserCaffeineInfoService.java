package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.drink.model.DrinkType;
import com.ktb.cafeboo.domain.drink.repository.DrinkTypeRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserCaffeineInfoService {

    private final UserRepository userRepository;
    private final UserCaffeineInfoRepository userCaffeineInfoRepository;
    private final DrinkTypeRepository drinkTypeRepository;

    @Transactional
    public UserCaffeineInfoCreateResponse create(Long userId, UserCaffeineInfoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (userCaffeineInfoRepository.existsByUserId(userId)) {
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserCaffeinInfo entity = UserCaffeineInfoMapper.toEntity(request, user);

            List<UserFavoriteDrinkType> favoriteDrinkTypes = Optional.ofNullable(request.getUserFavoriteDrinks())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(StringUtils::hasText) // 빈 문자열 방지 (스프링 유틸)
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
