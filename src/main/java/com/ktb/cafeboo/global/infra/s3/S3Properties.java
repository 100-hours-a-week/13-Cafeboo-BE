package com.ktb.cafeboo.global.infra.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.aws")
public record S3Properties(
        S3 s3,
        Credentials credentials,
        Region region
) {

    public record S3(String bucket, String dir, String defaultProfileImageUrl) {}
    public record Credentials(String accessKey, String secretKey) {}
    public record Region(String staticRegion) {}

    public String getAccessKey() {
        return credentials.accessKey();
    }

    public String getSecretKey() {
        return credentials.secretKey();
    }

    public String getRegion() {
        return region.staticRegion();
    }

    public String getBucket() {
        return s3.bucket();
    }

    public String getDir() {
        return s3.dir();
    }

    public String getDefaultProfileImageUrl() {
        return s3.defaultProfileImageUrl();
    }
}
