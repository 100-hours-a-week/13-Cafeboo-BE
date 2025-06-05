package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.*;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.Message;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.tag.service.TagService;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.CoffeeChatFilterType;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import com.ktb.cafeboo.global.util.AuthChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoffeeChatService {

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
    private final UserRepository userRepository;
    private final TagService tagService;

    public CoffeeChatCreateResponse create(Long userId, CoffeeChatCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        LocalDateTime meetingTime = LocalDateTime.of(request.date(), request.time());

        Location loc = request.location();

        CoffeeChat chat = CoffeeChat.builder()
                .writer(user)
                .name(request.title())
                .content(request.content())
                .status(CoffeeChatStatus.ACTIVE)
                .meetingTime(meetingTime)
                .maxMemberCount(request.memberCount())
                .currentMemberCount(1)
                .address(loc.address())
                .latitude(loc.latitude())
                .longitude(loc.longitude())
                .kakaoPlaceUrl(loc.kakaoPlaceUrl())
                .build();

        CoffeeChatMember hostMember = CoffeeChatMember.of (chat, user);
        chat.addMember(hostMember);

        CoffeeChat saved = coffeeChatRepository.save(chat);

        tagService.saveTagsToCoffeeChat(saved, request.tags());

        return new CoffeeChatCreateResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public CoffeeChatListResponse getCoffeeChatsByStatus(Long userId, String status) {
        CoffeeChatFilterType filter = CoffeeChatFilterType.from(status); // enum 파싱 및 예외처리 포함 권장

        List<CoffeeChat> chats;

        switch (filter) {
            case JOINED -> chats = coffeeChatRepository.findJoinedChats(userId);
            case COMPLETED -> chats = coffeeChatRepository.findCompletedChats(userId);
            case ALL -> chats = coffeeChatRepository.findAllActiveChats();
            default -> throw new CustomApiException(ErrorStatus.INVALID_COFFEECHAT_FILTER);
        }

        List<CoffeeChatListResponse.CoffeeChatSummary> summaryList = chats.stream()
                .map(chat -> {
                    boolean isJoined = chat.isJoinedBy(userId);
                    boolean isReviewed = chat.isReviewedBy(userId);

                    return CoffeeChatListResponse.CoffeeChatSummary.of(
                            chat,
                            isJoined,
                            isReviewed
                    );
                })
                .toList();

        return new CoffeeChatListResponse(filter.name().toLowerCase(), summaryList);
    }

    @Transactional(readOnly = true)
    public CoffeeChatDetailResponse getDetail(Long coffeechatId, Long userId) {
        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        return CoffeeChatDetailResponse.from(chat, userId);
    }

    @Transactional
    public CoffeeChatJoinResponse join(Long userId, Long coffeechatId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        if (!chat.getStatus().equals(CoffeeChatStatus.ACTIVE)) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_NOT_ACTIVE);
        }

        if (chat.isJoinedBy(userId)) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_ALREADY_JOINED);
        }

        if (chat.getCurrentMemberCount() >= chat.getMaxMemberCount()) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_CAPACITY_EXCEEDED);
        }

        CoffeeChatMember member = CoffeeChatMember.of(chat, user);
        chat.addMember(member);

        CoffeeChatMember saved = coffeeChatMemberRepository.save(member);
        return CoffeeChatJoinResponse.from(saved.getId());
    }

    @Transactional
    public void leaveChat(Long coffeechatId, Long memberId, Long userId) {
        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        CoffeeChatMember member = coffeeChatMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND));

        if (!member.getUser().getId().equals(userId)) {
            throw new CustomApiException(ErrorStatus.ACCESS_DENIED);
        }

        // 작성자는 나갈 수 없음
        if (chat.getWriter().getId().equals(userId)) {
            throw new CustomApiException(ErrorStatus.CANNOT_LEAVE_CHAT_OWNER);
        }

        chat.removeMember(member);
        coffeeChatMemberRepository.delete(member);

    }

    public void delete(Long coffeechatId, Long userId) {
        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        AuthChecker.checkOwnership(chat.getWriter().getId(), userId);

        if (chat.getStatus() != CoffeeChatStatus.ACTIVE) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_NOT_ACTIVE);
        }

        for (Message message : chat.getMessages()) {
            message.delete();
        }

        for (CoffeeChatMember member : chat.getMembers()) {
            member.delete();
        }

        chat.delete();
        coffeeChatRepository.save(chat);
    }
}
