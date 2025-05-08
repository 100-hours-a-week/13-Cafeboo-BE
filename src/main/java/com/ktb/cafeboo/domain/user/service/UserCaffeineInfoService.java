package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.*;
import com.ktb.cafeboo.domain.user.mapper.UserCaffeineInfoMapper;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeinInfo;
import com.ktb.cafeboo.domain.user.repository.UserCaffeineInfoRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserCaffeineInfoService {

    private final UserRepository userRepository;
    private final UserCaffeineInfoRepository userCaffeineInfoRepository;

    @Transactional
    public UserCaffeineInfoCreateResponse create(Long userId, UserCaffeineInfoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (userCaffeineInfoRepository.existsByUserId(userId)) {
            throw new CustomApiException(ErrorStatus.CAFFEINE_PROFILE_ALREADY_EXISTS);
        }

        try {
            UserCaffeinInfo entity = UserCaffeineInfoMapper.toEntity(request, user);
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
