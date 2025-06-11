package com.ktb.cafeboo.domain.coffeechat.service;


import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateRequest;
import com.ktb.cafeboo.domain.coffeechat.dto.CoffeeChatReviewCreateResponse;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChat;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatMember;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReview;
import com.ktb.cafeboo.domain.coffeechat.model.CoffeeChatReviewImage;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatRepository;
import com.ktb.cafeboo.domain.coffeechat.repository.CoffeeChatReviewRepository;
import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoffeeChatReviewService {
    private static final int MAX_IMAGE_COUNT = 3;

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatReviewRepository coffeeChatReviewRepository;
    private final CoffeeChatMemberRepository coffeeChatMemberRepository;
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
        if (LocalDateTime.now().isBefore(meetingTime) ) {
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

        return new CoffeeChatReviewCreateResponse(savedReview.getId().toString());
    }
}
