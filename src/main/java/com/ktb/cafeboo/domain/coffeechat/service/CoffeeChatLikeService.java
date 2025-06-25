package com.ktb.cafeboo.domain.coffeechat.service;

import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewLikeResponse;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatLike;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatLikeRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.domain.user.repository.UserRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.CoffeeChatLikeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoffeeChatLikeService {

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatLikeRepository coffeeChatLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public CoffeeChatReviewLikeResponse toggleLike(Long userId, Long coffeeChatId) {
        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeeChatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        CoffeeChatLike like = coffeeChatLikeRepository.findByCoffeeChatIdAndUserId(coffeeChatId, userId)
                .orElse(null);

        boolean liked;

        if (like == null) {
            // 좋아요 처음 누름
            User user = userRepository.getReferenceById(userId);
            CoffeeChatLike newLike = CoffeeChatLike.of(coffeeChat, user);
            coffeeChatLikeRepository.save(newLike);
            coffeeChat.increaseLikes();
            liked = true;

        } else if (like.getStatus() == CoffeeChatLikeStatus.CANCELLED) {
            // 좋아요 다시 누름
            like.setStatus(CoffeeChatLikeStatus.ACTIVE);
            coffeeChat.increaseLikes();
            liked = true;

        } else {
            // 좋아요 취소
            like.setStatus(CoffeeChatLikeStatus.CANCELLED);
            coffeeChat.decreaseLikes();
            liked = false;
        }

        return new CoffeeChatReviewLikeResponse(liked, coffeeChat.getLikesCount());
    }

    @Transactional(readOnly = true)
    public boolean hasLiked(Long userId, Long coffeeChatId) {
        return coffeeChatLikeRepository.existsByCoffeeChatIdAndUserIdAndStatus(
                coffeeChatId,
                userId,
                CoffeeChatLikeStatus.ACTIVE
        );
    }
}
