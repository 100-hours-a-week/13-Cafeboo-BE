package com.ktb.cafeboo.global.infra.ai.dto;

import lombok.Getter;

@Getter
public class PredictCaffeineLimitResponse {
    private String status;
    private String message;
    private Data data;

    @Getter
    public static class Data {
        private String userId;
        private float maxCaffeineMg;
    }
}
