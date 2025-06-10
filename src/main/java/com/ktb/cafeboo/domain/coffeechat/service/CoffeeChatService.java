package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.*;
import com.ktb.cafeboo.domain.coffeechat.dto.common.LocationDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoffeeChatService {

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
    private final UserRepository userRepository;
    private final TagService tagService;

    @Transactional
    public CoffeeChatCreateResponse create(Long userId, CoffeeChatCreateRequest request) {
        log.info("[CoffeeChatService.create] 커피챗 생성 요청: userId={}, title={}", userId, request.title());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.USER_NOT_FOUND));

        LocalDateTime meetingTime = LocalDateTime.of(request.date(), request.time());

        LocationDto loc = request.location();

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

        CoffeeChatMember hostMember = CoffeeChatMember.of(
                chat,
                user,
                request.chatNickname(),
                request.profileImageType()
        );
        chat.addMember(hostMember);

        CoffeeChat saved = coffeeChatRepository.save(chat);

        tagService.saveTagsToCoffeeChat(saved, request.tags());

        return new CoffeeChatCreateResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public CoffeeChatListResponse getCoffeeChatsByStatus(Long userId, String status) {
        log.info("[CoffeeChatService.getCoffeeChatsByStatus] 커피챗 목록 조회 요청: userId={}, status={}", userId, status);

        CoffeeChatFilterType filter;
        try {
            filter = CoffeeChatFilterType.from(status);
        } catch (IllegalArgumentException e) {
            log.warn("[CoffeeChatService.getCoffeeChatsByStatus] 유효하지 않은 필터값: userId={}, status={}", userId, status);
            throw new CustomApiException(ErrorStatus.INVALID_COFFEECHAT_FILTER);
        }

        List<CoffeeChat> chats = getChatsByFilter(filter, userId);
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
        log.info("[CoffeeChatService.getDetail] 커피챗 상세 조회 요청: chatId={}, userId={}", coffeechatId, userId);
        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        return CoffeeChatDetailResponse.from(chat, userId);
    }

    @Transactional
    public CoffeeChatJoinResponse join(Long userId, Long coffeechatId, CoffeeChatJoinRequest request) {
        log.info("[CoffeeChatService.join] 커피챗 참여 요청: userId={}, chatId={}", userId, coffeechatId);

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

        validateDuplicateNickname(coffeechatId, request.chatNickname());

        CoffeeChatMember member = CoffeeChatMember.of(
                chat,
                user,
                request.chatNickname(),
                request.profileImageType()
        );
        chat.addMember(member);

        CoffeeChatMember saved = coffeeChatMemberRepository.save(member);
        log.info("[CoffeeChatService.join] 참여자 닉네임: {}", request.chatNickname());
        return CoffeeChatJoinResponse.from(saved.getId());
    }

    @Transactional
    public void leaveChat(Long coffeechatId, Long memberId, Long userId) {
        log.info("[CoffeeChatService.leaveChat] 커피챗 나가기 요청: userId={}, chatId={}, memberId={}", userId, coffeechatId, memberId);

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

    @Transactional
    public void delete(Long coffeechatId, Long userId) {
        log.info("[CoffeeChatService.delete] 커피챗 삭제 요청: userId={}, chatId={}", userId, coffeechatId);

        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        AuthChecker.checkOwnership(chat.getWriter().getId(), userId);

        if (chat.getStatus() != CoffeeChatStatus.ACTIVE) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_NOT_ACTIVE);
        }

        for (CoffeeChatMessage message : chat.getMessages()) {
            message.delete();
        }

        for (CoffeeChatMember member : chat.getMembers()) {
            member.delete();
        }

        chat.delete();
        coffeeChatRepository.save(chat);
    }

    private List<CoffeeChat> getChatsByFilter(CoffeeChatFilterType filter, Long userId) {
        return switch (filter) {
            case JOINED -> coffeeChatRepository.findJoinedChats(userId);
            case ENDED -> coffeeChatRepository.findCompletedChats(userId);
            case ALL -> coffeeChatRepository.findAllActiveChats();
            case REVIEWABLE -> coffeeChatRepository.findReviewableChats(userId);
        };
    }

    private void validateDuplicateNickname(Long coffeechatId, String chatNickname) {
        boolean exists = coffeeChatMemberRepository.existsByCoffeeChatIdAndChatNickname(coffeechatId, chatNickname);
        if (exists) {
            throw new CustomApiException(ErrorStatus.CHAT_NICKNAME_ALREADY_EXISTS);
        }
    }
}
