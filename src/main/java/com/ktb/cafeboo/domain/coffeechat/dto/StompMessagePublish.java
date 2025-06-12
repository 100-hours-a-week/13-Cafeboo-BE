package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMessage;
import com.ktb.cafeboo.global.enums.MessageType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StompMessagePublish {
    private String messageId; // 메시지 고유 ID (UUID)
    private Long coffeechatId;
    private MessageType messageType; // 메시지 타입 (TALK, SYSTEM 등)
    private String content; // 메시지 내용
    private LocalDateTime sentAt; // 메시지 전송 시간 (ISO 8601 형식으로 직렬화될 것임)
    private SenderInfo sender; // 중첩된 Sender 객체

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SenderInfo { // 내부 클래스로 Sender 정보 정의
        private String userId; // 사용자의 고유 ID (String 형태)
        private String name; // 사용자 이름 또는 닉네임
        private String profileImageUrl; // 사용자 프로필 이미지 URL
    }

    // Factory method (옵션: 엔티티로부터 DTO 생성 편의 메서드)
    public static StompMessagePublish from(CoffeeChatMessage message, CoffeeChatMember senderMember) {
        return StompMessagePublish.builder()
            .messageId(message.getMessageUuid())
            .coffeechatId(message.getCoffeeChat().getId())
            .messageType(message.getType())
            .content(message.getContent())
            .sentAt(message.getCreatedAt()) // BaseEntity의 createdAt 필드 사용
            .sender(SenderInfo.builder()
                .userId(String.valueOf(senderMember.getUser().getId())) // User ID를 String으로 변환
                .name(senderMember.getChatNickname()) // CoffeeChatMember의 닉네임 사용
                .profileImageUrl("") // CoffeeChatMember의 프로필 이미지 사용
                .build())
            .build();
    }
}
