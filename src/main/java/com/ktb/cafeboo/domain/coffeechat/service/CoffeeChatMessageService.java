package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatMessagesResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MessageDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.SenderDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMessageRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.CoffeeChatStatus;
import com.ktb.cafeboo.global.enums.ProfileImageType;
import com.ktb.cafeboo.global.infra.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final S3Properties s3Properties;

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
        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages = messages.subList(0, limit);
        }

        List<MessageDto> messageDtos = messages.stream()
                .map(m -> {
                    CoffeeChatMember sender = m.getSender();
                    String profileImageUrl = sender.getProfileImageType() == ProfileImageType.DEFAULT
                            ? s3Properties.getDefaultProfileImageUrl()
                            : sender.getUser().getProfileImageUrl();

                    return new MessageDto(
                            m.getMessageUuid(),
                            new SenderDto(
                                    sender.getId(),
                                    sender.getChatNickname(),
                                    profileImageUrl
                            ),
                            m.getContent(),
                            m.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());

        String nextCursor = hasNext ? messageDtos.get(messageDtos.size() - 1).messageId() : null;

        return new CoffeeChatMessagesResponse(
                coffeechatId,
                messageDtos,
                nextCursor,
                hasNext
        );
    }

    private List<CoffeeChatMessage> fetchMessagesByCursor(Long chatId, String cursor, int limit, String order) {
        PageRequest pageRequest = PageRequest.of(0, limit + 1);

        if (cursor == null) {
            return messageRepository.findByCoffeeChatId(chatId)
                    .stream()
                    .sorted(order.equalsIgnoreCase("asc")
                            ? java.util.Comparator.comparing(CoffeeChatMessage::getId)
                            : java.util.Comparator.comparing(CoffeeChatMessage::getId).reversed())
                    .limit(limit + 1)
                    .collect(Collectors.toList());
        }

        Long cursorId;
        try {
            cursorId = Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new CustomApiException(ErrorStatus.INVALID_CURSOR);
        }

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
