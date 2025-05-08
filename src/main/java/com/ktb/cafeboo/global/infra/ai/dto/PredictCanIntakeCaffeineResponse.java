package com.ktb.cafeboo.global.infra.ai.dto;

import lombok.Getter;

@Getter
public class PredictCanIntakeCaffeineResponse {
    private String status;
    private String message;
    private Data data;

    @Getter
    public static class Data {
        private String userId;
        private String caffeineStatus;  // "Y" or "N"
    }
}
