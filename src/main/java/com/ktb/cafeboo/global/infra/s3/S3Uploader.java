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

    public String uploadProfileImage(InputStream inputStream, long contentLength, String contentType) {
        String fileName = String.format("%s/profile-images/%s", s3Properties.getDir(), UUID.randomUUID());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(fileName)
                    .contentType(contentType)
                    .acl("public-read")
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            String uploadedUrl = generatePublicUrl(fileName);
            log.info("[S3Uploader.uploadProfileImageOrDefault] 업로드 성공: {}", uploadedUrl);
            return uploadedUrl;
        } catch (Exception e) {
            log.warn("[S3Uploader.uploadProfileImageOrDefault] 업로드 실패, 기본 이미지 사용: {}", e.getMessage());
            return s3Properties.getDefaultProfileImageUrl();
        }
    }

    public String uploadReviewImage(InputStream inputStream, long contentLength, String contentType) {
        String fileName = String.format("%s/review-images/%s", s3Properties.getDir(), UUID.randomUUID());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(fileName)
                    .contentType(contentType)
                    .acl("public-read")
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

            String uploadedUrl = generatePublicUrl(fileName);
            log.info("[S3Uploader.uploadReviewImage] 업로드 성공: {}", uploadedUrl);
            return uploadedUrl;
        } catch (Exception e) {
            log.error("[S3Uploader.uploadReviewImage] 업로드 실패", e);
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
