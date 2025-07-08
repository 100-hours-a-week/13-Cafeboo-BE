package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.auth.repository.OauthTokenRepository;
import com.ktb.cafeboo.domain.auth.service.TokenBlacklistService;
import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileUpdateRequest;
import com.ktb.cafeboo.domain.user.dto.UserProfileUpdateResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.model.UserCaffeineInfo;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.LoginType;
import com.ktb.cafeboo.global.enums.TokenBlacklistReason;
import com.ktb.cafeboo.global.enums.UserRole;
import com.ktb.cafeboo.global.infra.s3.S3Uploader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OauthTokenRepository oauthTokenRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final S3Uploader s3Uploader;

    @Transactional(readOnly = true)
    public User findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public EmailDuplicationResponse isEmailDuplicated(String email) {
        boolean isDuplicated = userRepository.existsByEmail(email);
        log.info("[UserService.isEmailDuplicated] 이메일 중복 확인 - email={}, duplicated={}", email, isDuplicated);
        return new EmailDuplicationResponse(email, isDuplicated);
    }

    @Transactional(readOnly = true)
    public boolean hasCompletedOnboarding(User user) {
        return user.getHealthInfo() != null
                && user.getCaffeinInfo() != null;
    }

    @Transactional(readOnly = true)
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
                targetUser.getProfileImageUrl(),
                (int) dailyCaffeineLimit,
                targetUser.getCoffeeBean(),
                0
        );
    }

    @Transactional
    public UserProfileUpdateResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            user.updateNickname(request.nickname());
        }

        if (request.profileImage() != null) {
            MultipartFile file = request.profileImage();
            try (InputStream inputStream = file.getInputStream()) {
                String url = s3Uploader.uploadProfileImage(inputStream, file.getSize(), file.getContentType());
                user.updateProfileImage(url);
            } catch (IOException e) {
                throw new CustomApiException(ErrorStatus.S3_PROFILE_IMAGE_UPLOAD_FAILED);
            }
        }
        userRepository.save(user);

        return new UserProfileUpdateResponse(userId.toString(), user.getUpdatedAt());
    }

    @Transactional
    public void deleteUser(String accessToken, Long userId) {
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

        try {
            tokenBlacklistService.addToBlacklist(accessToken, userId.toString(), TokenBlacklistReason.WITHDRAWAL);
        } catch (Exception e) {
            log.warn("accessToken 블랙리스트 등록 실패: {}", e.getMessage());
        }
      
        log.info("[UserService.deleteUser] 사용자 soft delete 완료 - userId={}", userId);
    }

    @Transactional
    public User createGuestUser() {
        String guestToken = UUID.randomUUID().toString();
        String nickname = "guest_" + guestToken.substring(0, 3);

        User guest = User.builder()
                .loginType(LoginType.GUEST)
                .nickname(nickname)
                .role(UserRole.GUEST)
                .darkMode(false)
                .coffeeBean(0)
                .profileImageUrl(s3Uploader.getDefaultProfileImageUrl())
                .refreshToken("")
                .build();

        UserCaffeineInfo caffeineInfo = UserCaffeineInfo.builder()
                .user(guest)
                .caffeineSensitivity(0)
                .averageDailyCaffeineIntake(0f)
                .frequentDrinkTime(null)
                .dailyCaffeineLimitMg(400.0f)         // 기본값
                .sleepSensitiveThresholdMg(100.0f)    // 기본값
                .build();

        guest.setCaffeinInfo(caffeineInfo);

        return userRepository.save(guest);
    }
}