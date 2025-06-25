package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ToxicityDetectionRequest {

    @JsonProperty("user_input")
    private final String userInput;
}
