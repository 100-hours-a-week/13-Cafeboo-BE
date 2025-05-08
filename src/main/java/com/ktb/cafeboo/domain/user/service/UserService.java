package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    /**
     * 새로운 유저를 저장합니다.
     *
     * @param user 저장할 유저 객체
     */
    public void saveUser(User user){
        userRepository.save(user);
    }

    /**
     * 주어진 ID에 해당하는 유저 정보를 조회합니다.
     *
     * @param id 조회할 유저의 ID
     * @return 주어진 ID에 해당하는 유저 객체
     * @throws IllegalArgumentException 해당 ID를 가진 유저 정보가 존재하지 않을 경우 발생합니다.
     */
    public User findUserById(Long id){
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("해당 아이디를 가진 유저 정보가 존재하지 않습니다"));
    }
  
    public EmailDuplicationResponse isEmailDuplicated(String email) {
        boolean isDuplicated = userRepository.existsByEmail(email);
        return new EmailDuplicationResponse(email, isDuplicated);
    }

    public boolean hasCompletedOnboarding(User user) {
        return user.getHealthInfo() != null
                && user.getCaffeinInfo() != null
                && user.getAlarmSetting() != null;
    }
  
    public UserProfileResponse getUserProfile(Long targetUserId, Long currentUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));


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
  
  //    public UserProfileResponse getUserProfile(Long targetUserId, Long currentUserId) {
//        User targetUser = userRepository.findById(targetUserId)
//                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));
//
//        AuthChecker.checkOwnership(targetUser.getId(), currentUserId);
//
//        return new UserProfileResponse(
//                targetUser.getNickname(),
//                targetUser.getDailyCaffeineLimitMg(),
//                targetUser.getCoffeeBean(),
//                challengeRepository.countByUserId(targetUserId)
//        );
//    }
}
