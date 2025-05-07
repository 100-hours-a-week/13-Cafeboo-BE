package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateRequest;
import com.ktb.cafeboo.domain.user.dto.UserHealthInfoCreateResponse;
import com.ktb.cafeboo.domain.user.mapper.UserHealthInfoMapper;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserHealthInfo;
import com.ktb.cafeboo.domain.user.repository.UserHealthInfoRepository;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserHealthInfoService {

    private final UserRepository userRepository;
    private final UserHealthInfoRepository userHealthInfoRepository;

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
                    .userId(user.getId())
                    .createdAt(entity.getCreatedAt())
                    .build();

        } catch (Exception e) {
            // 매핑/저장 중 오류 발생 시 BAD_REQUEST 반환
            throw new CustomApiException(ErrorStatus.BAD_REQUEST);
        }
    }
}
