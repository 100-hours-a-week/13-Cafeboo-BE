package com.ktb.cafeboo.domain.coffeechat.dto;

import com.ktb.cafeboo.domain.coffeechat.dto.common.MemberDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;

import java.util.List;

public record CoffeeChatListResponse(
        String filter,
        List<CoffeeChatSummary> coffeechats
) {
    public record CoffeeChatSummary(
            String coffeeChatId,
            String title,
            String date,
            String time,
            int maxMemberCount,
            int currentMemberCount,
            List<String> tags,
            String address,
            MemberDto writer,

            // 조건부 필드
            Boolean isJoined,
            Boolean isReviewed
    ) {
        public static CoffeeChatSummary of(
                com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat chat,
                boolean isJoined,
                boolean isReviewed
        ) {
            MemberDto writerDto = chat.getMembers().stream()
                // chat.getWriter()가 null일 가능성도 고려하여 필터 조건 추가
                .filter(m -> chat.getWriter() != null && m.getUser().getId().equals(chat.getWriter().getId()))
                .findFirst()
                // CoffeeChatMember를 찾았다면, MemberDto로 변환합니다.
                .map(m -> new MemberDto(
                    m.getId().toString(),
                    m.getChatNickname(),
                    m.getProfileImageUrl(),
                    m.isHost()
                ))
                // Optional이 비어있을 경우 (즉, 작성자 멤버를 찾지 못했을 경우) 실행됩니다.
                .orElseGet(() -> {
                    // 기본 MemberDto 객체를 생성하여 반환합니다.
                    return new MemberDto(
                        "unknown", // 유니크하거나 의미 있는 더미 ID
                        "알 수 없는 작성자", // 기본 닉네임
                        "default_profile_image_url.png", // 실제 존재하는 기본 이미지 URL로 변경 권장
                        false // 호스트 여부 (알 수 없으므로 false)
                    );
                });


            return new CoffeeChatSummary(
                    chat.getId().toString(),
                    chat.getName(),
                    chat.getMeetingTime().toLocalDate().toString(),
                    chat.getMeetingTime().toLocalTime().toString(),
                    chat.getMaxMemberCount(),
                    chat.getCurrentMemberCount(),
                    chat.getTagNames(),
                    chat.getAddress(),
                    writerDto,
                    isJoined,
                    isReviewed
            );
        }
    }
}

