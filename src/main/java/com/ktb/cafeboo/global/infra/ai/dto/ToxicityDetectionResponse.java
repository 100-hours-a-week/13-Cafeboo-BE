package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ToxicityDetectionResponse {
    private String status;
    private String message;
    @JsonProperty("is_toxic")
    private int isToxic;
}