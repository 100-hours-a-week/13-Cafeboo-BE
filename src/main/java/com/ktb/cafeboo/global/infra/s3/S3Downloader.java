package com.ktb.cafeboo.global.infra.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Downloader {

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public List<String> downloadKeywordLines(String filename) {
        String fullKey = s3Properties.getDir() + "/text-file/" + filename;
        log.info("[S3Downloader] 요청한 S3 Key: {}", fullKey);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(fullKey)
                    .build();

            try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8))) {

                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            log.error("[S3Downloader] 금칙어 파일 다운로드 실패 - filename: {}, 이유: {}", filename, e.getMessage(), e);
            throw new RuntimeException("S3에서 키워드 파일 다운로드 실패", e);
        }
    }
}
