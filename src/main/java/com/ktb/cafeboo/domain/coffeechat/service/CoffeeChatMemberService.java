package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatMembershipCheckResponse;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoffeeChatMemberService {

    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
    private final CoffeeChatRepository coffeeChatRepository;

    public CoffeeChatMembershipCheckResponse checkMembership(Long coffeechatId, Long userId) {
        if (!coffeeChatRepository.existsById(coffeechatId)) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND);
        }

        Optional<CoffeeChatMember> memberOpt =
                coffeeChatMemberRepository.findByCoffeeChatIdAndUserId(coffeechatId, userId);

        if (memberOpt.isPresent()) {
            return CoffeeChatMembershipCheckResponse.builder()
                    .isMember(true)
//                    .memberId(String.valueOf(memberOpt.get().getId()))
                    .memberId(String.valueOf(userId))
                    .build();
        } else {
            return CoffeeChatMembershipCheckResponse.builder()
                    .isMember(false)
                    .build();
        }
    }
}