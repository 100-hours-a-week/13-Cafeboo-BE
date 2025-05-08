package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.util.AuthChecker;
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
