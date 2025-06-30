package com.ktb.cafeboo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.multipart.MultipartFile;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileUpdateRequest(
        String nickname,
        MultipartFile profileImage
) {}
