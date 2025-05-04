package com.ktb.cafeboo.domain.report.dto;

import lombok.Builder;
import lombok.Getter;
//import lombok.NoArgsConstructor;  // NoArgsConstructor 제거
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class DailyCaffeineReportResponse {
    private String nickname;
    private float dailyCaffeineLimit;
    private float dailyCaffeineIntakeMg;
    private int dailyCaffeineIntakeRate;
    private String intakeGuide;
    private float sleepSensitiveThreshold;
    private List<HourlyCaffeineInfo> caffeineByHour;

    @Getter
    @AllArgsConstructor
    public static class HourlyCaffeineInfo {
        private String time;        // "HH:00" 형식
        private float caffeineMg;   // 해당 시간의 카페인 잔여량
    }
}
