package com.ktb.cafeboo.global.infra.s3;

import com.ktb.cafeboo.global.apiPayload.code.status.ErrorStatus;
import com.ktb.cafeboo.global.apiPayload.exception.CustomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    private String upload(String directory, InputStream inputStream, long contentLength, String contentType) {
        String fileName = String.format("%s/%s/%s", s3Properties.getDir(), directory, UUID.randomUUID());

        log.info("[S3Uploader.upload] 업로드 시작 - key: {}, contentLength: {}, contentType: {}", fileName, contentLength, contentType);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            String uploadedUrl = generatePublicUrl(fileName);
            log.info("[S3Uploader.upload] 업로드 성공 - URL: {}", uploadedUrl);
            return uploadedUrl;
        } catch (Exception e) {
            log.error("[S3Uploader.upload] 업로드 실패 - key: {}, 이유: {}", fileName, e.getMessage(), e);
            throw e;
        }
    }

    public String uploadProfileImage(InputStream inputStream, long contentLength, String contentType) {
        try {
            return upload("profile-uploads", inputStream, contentLength, contentType);
        } catch (Exception e) {
            log.warn("[S3Uploader.uploadProfileImage] 프로필 이미지 업로드 실패. 기본 이미지로 대체합니다. reason={}", e.getMessage());
            return s3Properties.getDefaultProfileImageUrl();
        }
    }

    public String uploadReviewImage(InputStream inputStream, long contentLength, String contentType) {
        try {
            return upload("review-uploads", inputStream, contentLength, contentType);
        } catch (Exception e) {
            log.error("[S3Uploader.uploadReviewImage] 리뷰 이미지 업로드 실패. reason={}", e.getMessage(), e);
            throw new CustomApiException(ErrorStatus.S3_REVIEW_IMAGE_UPLOAD_FAILED);
        }
    }

    public String generatePresignedUrl(String fileName, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String generatePublicUrl(String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", s3Properties.getBucket(), key);
    }

    public String getDefaultProfileImageUrl() {
        return s3Properties.getDefaultProfileImageUrl();
    }

}
