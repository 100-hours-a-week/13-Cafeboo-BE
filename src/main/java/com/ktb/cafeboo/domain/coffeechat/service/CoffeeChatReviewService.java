package com.ktb.cafeboo.domain.coffeechat.service;


import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateRequest;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewListResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewResponse;
import com.ktb.cafeboo.domain.coffeechat.dto.common.CoffeeChatReviewPreviewDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.MemberDto;
import com.ktb.cafeboo.domain.coffeechat.dto.common.ReviewDto;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReview;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReviewImage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatReviewImageRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatReviewRepository;
import com.ktb.cafeboo.domain.tag.model.CoffeeChatTag;
import com.ktb.cafeboo.domain.tag.repository.CoffeeChatTagRepository;
import com.ktb.cafeboo.domain.tag.repository.TagRepository;
import com.ktb.cafeboo.domain.user.model.User;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import com.ktb.cafeboo.global.enums.ReviewFilterType;
import com.ktb.cafeboo.global.infra.s3.S3Uploader;
import com.ktb.cafeboo.global.util.AuthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoffeeChatReviewService {
    private static final int MAX_IMAGE_COUNT = 3;
    private static final int REWARD_BEANS_WITH_IMAGE = 3;
    private static final int REWARD_BEANS_TEXT_ONLY = 1;

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatReviewRepository coffeeChatReviewRepository;
    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
    private final CoffeeChatReviewImageRepository coffeeChatReviewImageRepository;
    private final CoffeeChatLikeService coffeeChatLikeService;
    private final TagRepository tagRepository;
    private final CoffeeChatTagRepository coffeeChatTagRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public CoffeeChatReviewCreateResponse createCoffeeChatReview(
            Long userId,
            Long coffeechatId,
            CoffeeChatReviewCreateRequest request
    ) {

        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeechatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        CoffeeChatMember writer = coffeeChatMemberRepository.findById(Long.parseLong(request.memberId()))
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_MEMBER_NOT_FOUND));
        AuthChecker.checkOwnership(writer.getUser().getId(), userId);

        if (!writer.getCoffeeChat().getId().equals(coffeechatId)) {
            throw new CustomApiException(ErrorStatus.MEMBER_NOT_IN_THIS_CHAT);
        }

        boolean isAlreadyReviewed = coffeeChatReviewRepository.existsByWriter(writer);
        if (isAlreadyReviewed) {
            throw new CustomApiException(ErrorStatus.ALREADY_REVIEWED);
        }

        LocalDateTime meetingTime = coffeeChat.getMeetingTime();
        //LocalDateTime reviewDeadline = meetingTime.plusHours(24); 후기 작성 데드라인은 현재 없음
        if (LocalDateTime.now().plusHours(9).isBefore(meetingTime)) {
            throw new CustomApiException(ErrorStatus.INVALID_REVIEW_TIME);
        }

        // 이미지 처리
        List<MultipartFile> files = request.images() != null ? request.images() : List.of();

        if (files.size() > MAX_IMAGE_COUNT) {
            throw new CustomApiException(ErrorStatus.INVALID_IMAGE_COUNT);
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try (InputStream inputStream = file.getInputStream()) {
                String url = s3Uploader.uploadReviewImage(inputStream, file.getSize(), file.getContentType());
                imageUrls.add(url);
            } catch (IOException e) {
                throw new CustomApiException(ErrorStatus.S3_REVIEW_IMAGE_UPLOAD_FAILED);
            }
        }

        List<CoffeeChatReviewImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(CoffeeChatReviewImage.of(null, imageUrls.get(i), i));
        }

        CoffeeChatReview review = CoffeeChatReview.of(
                coffeeChat,
                writer,
                request.text(),
                images
        );
        CoffeeChatReview savedReview = coffeeChatReviewRepository.save(review);

        // 커피콩 지급
        boolean hasImage = !images.isEmpty();
        int rewardCount = hasImage ? REWARD_BEANS_WITH_IMAGE : REWARD_BEANS_TEXT_ONLY;

        User user = writer.getUser();
        user.addCoffeeBeans(rewardCount);

        return new CoffeeChatReviewCreateResponse(savedReview.getId().toString());
    }

    @Transactional(readOnly = true)
    public CoffeeChatReviewListResponse getCoffeeChatReviewsByStatus(Long userId, String status) {
        ReviewFilterType filter = ReviewFilterType.from(status);

        List<CoffeeChat> chats;

        switch (filter) {
            case MY -> chats = coffeeChatRepository.findChatsWithReviewsByUserId(userId);
            case ALL -> chats = coffeeChatRepository.findAllWithReviews();
            default -> throw new CustomApiException(ErrorStatus.INVALID_REVIEW_FILTER);
        }
        List<Long> chatIds = chats.stream().map(CoffeeChat::getId).toList();
        List<Long> reviewIds = chats.stream()
                .flatMap(c -> c.getReviews().stream())
                .map(CoffeeChatReview::getId)
                .toList();

        // 좋아요 상태: 모든 chatId에 대해 한 번에 조회
        Map<Long, Boolean> likedMap = coffeeChatLikeService.bulkCheckLiked(userId, chatIds);

        // 리뷰 이미지: 리뷰 ID 기준으로 일괄 조회
        Map<Long, List<CoffeeChatReviewImage>> imageMap = coffeeChatReviewImageRepository.findAllByReviewIds(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getReview().getId()));

        // 태그 매핑: chatId 기준으로 조회
        List<CoffeeChatTag> coffeeChatTags = coffeeChatTagRepository.findByChatIds(chatIds);
        Map<Long, List<CoffeeChatTag>> tagMap = coffeeChatTags.stream()
                .collect(Collectors.groupingBy(t -> t.getCoffeeChat().getId()));

        // tagId 전부 수집
        Set<Long> tagIds = coffeeChatTags.stream()
                .map(t -> t.getTag().getId())
                .collect(Collectors.toSet());

        // 태그 ID → 이름 매핑
        Map<Long, String> tagNameMap = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(t -> t.getId(), t -> t.getName()));

        List<CoffeeChatReviewPreviewDto> dtos = chats.stream()
                .filter(chat -> !chat.getReviews().isEmpty())
                .map(chat -> {
                    List<CoffeeChatReview> reviews = new ArrayList<>(chat.getReviews());

                    List<String> tagNames = tagMap.getOrDefault(chat.getId(), List.of()).stream()
                            .map(t -> tagNameMap.get(t.getTag().getId()))
                            .filter(Objects::nonNull)
                            .toList();


                    int totalImageCount = reviews.stream()
                            .mapToInt(r -> imageMap.getOrDefault(r.getId(), List.of()).size())
                            .sum();

                    String previewImageUrl = reviews.stream()
                            .map(r -> imageMap.getOrDefault(r.getId(), List.of()))
                            .filter(list -> !list.isEmpty())
                            .map(list -> list.get(0).getImageUrl())
                            .findFirst()
                            .orElse(null);

                    boolean liked = likedMap.getOrDefault(chat.getId(), false);

                    return CoffeeChatReviewPreviewDto.from(chat, tagNames,totalImageCount, previewImageUrl, liked);
                })
                .toList();

        return new CoffeeChatReviewListResponse(
                filter.name().toLowerCase(),
                dtos.size(),
                dtos
        );
    }

    public CoffeeChatReviewResponse getReviewByCoffeeChatId(Long userId, Long coffeeChatId) {
        CoffeeChat coffeeChat = coffeeChatRepository.findWithDetailsById(coffeeChatId)
                .orElseThrow(() -> new CustomApiException(ErrorStatus.COFFEECHAT_NOT_FOUND));

        List<ReviewDto> reviewDtos = coffeeChat.getReviews().stream()
                .map(review -> new ReviewDto(
                        String.valueOf(review.getId()),
                        review.getText(),
                        review.getImages().stream()
                                .map(CoffeeChatReviewImage::getImageUrl)
                                .toList(),
                        new MemberDto(
                                String.valueOf(review.getWriter().getId()),
                                review.getWriter().getChatNickname(),
                                review.getWriter().getProfileImageUrl(),
                                review.getWriter().isHost()
                        )
                ))
                .toList();

        boolean liked = coffeeChatLikeService.hasLiked(userId, coffeeChat.getId());

        return new CoffeeChatReviewResponse(
                String.valueOf(coffeeChat.getId()),
                coffeeChat.getName(),
                coffeeChat.getMeetingTime().toLocalDate().toString(),
                coffeeChat.getMeetingTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                coffeeChat.getTagNames(),
                coffeeChat.getAddress(),
                coffeeChat.getLikesCount(),
                liked,
                reviewDtos
        );
    }
}
