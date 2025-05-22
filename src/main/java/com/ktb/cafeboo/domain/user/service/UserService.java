package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.auth.repository.OauthTokenRepository;
import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OauthTokenRepository oauthTokenRepository;

    public User findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));
    }

    public EmailDuplicationResponse isEmailDuplicated(String email) {
        boolean isDuplicated = userRepository.existsByEmail(email);
        log.info("[UserService.isEmailDuplicated] 이메일 중복 확인 - email={}, duplicated={}", email, isDuplicated);
        return new EmailDuplicationResponse(email, isDuplicated);
    }

    public boolean hasCompletedOnboarding(User user) {
        return user.getHealthInfo() != null
                && user.getCaffeinInfo() != null;
    }
  
    public UserProfileResponse getUserProfile(Long targetUserId, Long currentUserId) {
        log.info("[UserService.getUserProfile] 사용자 프로필 조회 - targetUserId={}, currentUserId={}", targetUserId, currentUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> {
                    log.warn("[UserService.getUserProfile] 존재하지 않는 사용자 - userId={}", targetUserId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        float dailyCaffeineLimit = targetUser.getCaffeinInfo() != null
                ? targetUser.getCaffeinInfo().getDailyCaffeineLimitMg()
                : 400.0f;

        // int challengeCount = challengeRepository.countByUserId(targetUserId); // TODO: 챌린지 추가 이후 실제 구현 필요

        return new UserProfileResponse(
                targetUser.getNickname(),
                (int) dailyCaffeineLimit,
                targetUser.getCoffeeBean(),
                0
        );
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("[UserService.deleteUser] 회원 탈퇴 처리 시작 - userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[UserService.deleteUser] 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomApiException(ErrorStatus.USER_NOT_FOUND);
                });

        if (user.getHealthInfo() != null) {
            user.getHealthInfo().delete();
        }

        if (user.getCaffeinInfo() != null) {
            user.getCaffeinInfo().delete();
        }

        if (user.getAlarmSetting() != null) {
            user.getAlarmSetting().delete();
        }

        if (user.getFavoriteDrinks() != null && !user.getFavoriteDrinks().isEmpty()) {
            user.getFavoriteDrinks().clear();
        }

        user.setRefreshToken(null);
        oauthTokenRepository.deleteByUserId(userId);
        log.info("[UserService.deleteUser] Oauth 토큰 제거 완료 - userId={}", userId);

        user.delete();
        userRepository.save(user);
        log.info("[UserService.deleteUser] 사용자 soft delete 완료 - userId={}", userId);
    }
}