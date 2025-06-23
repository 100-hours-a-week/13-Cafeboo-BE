package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatMessagesResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MessageDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MemberDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMessageRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoffeeChatMessageService {

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatMessageRepository messageRepository;
    private final CoffeeChatMemberRepository memberRepository;

    @Value("${DEFAULT_PROFILE_IMAGE_URL}")
    private String defaultProfileImageUrl;

    public CoffeeChatMessagesResponse getMessages(Long userId, Long coffeechatId, String cursor, int limit, String order) {
        log.info("[CoffeeChatMessageService.getMessages] 커피챗 메시지 조회 요청: userId={}, chatId={}, cursor={}, limit={}, order={}",
                userId, coffeechatId, cursor, limit, order);

        CoffeeChat chat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        if (!chat.getStatus().equals(CoffeeChatStatus.ACTIVE)) {
            throw new CustomApiException(ErrorStatus.COFFEECHAT_NOT_ACTIVE);
        }

        CoffeeChatMember member = memberRepository.findByCoffeeChatIdAndUserId(coffeechatId, userId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND));

        List<CoffeeChatMessage> messages = fetchMessagesByCursor(coffeechatId, cursor, limit, order);
        log.info("[CoffeeChatMessageService.getMessages] 커피챗 메시지 조회 크기 : {}", messages.size());

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages = messages.subList(0, limit);
        }

        List<MessageDto> messageDtos = messages.stream()
                .map(m -> {
                    CoffeeChatMember sender = m.getSender();

                    // Optional.ofNullable을 사용하여 sender가 null인 경우를 안전하게 처리
                    String senderId = Optional.ofNullable(sender)
                        .map(s -> s.getId().toString())
                        .orElse(null); // sender가 null이면 ID도 null

                    String senderChatNickname = Optional.ofNullable(sender)
                        .map(CoffeeChatMember::getChatNickname)
                        .orElse("<알 수 없음>"); // sender가 null이면 "<알수없음>"

                    String senderProfileImageUrl = Optional.ofNullable(sender)
                        .map(CoffeeChatMember::getProfileImageUrl)
                        .orElse(defaultProfileImageUrl); // sender가 null이면 기본 이미지 URL
                    // 예: "/images/default_profile.png"

                    boolean senderIsHost = Optional.ofNullable(sender)
                        .map(CoffeeChatMember::isHost)
                        .orElse(false); // sender가 null이면 호스트 아님

                    return new MessageDto(
                            String.valueOf(m.getId()),
                            new MemberDto(
                                senderId,
                                senderChatNickname,
                                senderProfileImageUrl,
                                senderIsHost
                            ),
                            m.getContent(),
                            m.getType(),
                            m.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        Collections.reverse(messageDtos);
        log.info("[CoffeeChatMessageService.getMessages] 커피챗 메시지 DTO 크기 : {}", messageDtos.size());

        String nextCursor =
            !messageDtos.isEmpty() ? String.valueOf(messageDtos.getFirst().messageId()) : "0";

        return new CoffeeChatMessagesResponse(
                coffeechatId.toString(),
                messageDtos,
                nextCursor,
                hasNext
        );
    }

    private List<CoffeeChatMessage> fetchMessagesByCursor(Long chatId, String cursor, int limit, String order) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);

        if (cursor == null || cursor.equals("0")) {
            log.info("[CoffeeChatMessageService.fetchMessageByCursor] - cursor null 분기 실행");
            return messageRepository.findByCoffeeChatId(chatId)
                    .stream()
                    .sorted(order.equalsIgnoreCase("asc")
                            ? java.util.Comparator.comparing(CoffeeChatMessage::getId)
                            : java.util.Comparator.comparing(CoffeeChatMessage::getId).reversed())
                    .limit(limit + 1)
                    .collect(Collectors.toList());
        }

        log.info("[CoffeeChatMessageService.fetchMessageByCursor] - cursor is not null 분기 실행");
        Long cursorId;
        try {
            cursorId = Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new CustomApiException(ErrorStatus.INVALID_CURSOR);
        }
        log.info("[CoffeeChatMessageService.fetchMessageByCursor] - cursorId: {}", cursorId);
        if (order.equalsIgnoreCase("asc")) {
            return messageRepository.findByCoffeeChatIdAndIdGreaterThanOrderByIdAsc(chatId, cursorId, pageRequest);
        } else {
            return messageRepository.findByCoffeeChatIdAndIdLessThanOrderByIdDesc(chatId, cursorId, pageRequest);
        }
    }

    public CoffeeChatMessage save(CoffeeChatMessage coffeeChatMessage){
        CoffeeChatMessage savedMessage = messageRepository.save(coffeeChatMessage);
        return savedMessage;
    }
}
