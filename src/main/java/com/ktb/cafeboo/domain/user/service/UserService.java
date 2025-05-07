package com.ktb.cafeboo.domain.user.service;

import com.ktb.cafeboo.domain.user.dto.EmailDuplicationResponse;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
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
}
