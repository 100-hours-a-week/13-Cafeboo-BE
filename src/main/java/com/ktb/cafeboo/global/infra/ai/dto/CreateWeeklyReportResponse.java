package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CreateWeeklyReportResponse {
    private String status;
    private String message;
    private Data data;

    @Getter
    public static class Data {
        @JsonProperty("user_id")
        private String userId;
        private String report;
    }
}
