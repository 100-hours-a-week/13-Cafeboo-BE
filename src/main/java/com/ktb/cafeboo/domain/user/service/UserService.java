package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.dto.UserProfileResponse;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.util.AuthChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
