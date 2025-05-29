package com.ktb.cafeboo.domain.report.dto;

import java.util.List;

public record DailyCaffeineReportResponse(
        String nickname,
        float dailyCaffeineLimit,
        float dailyCaffeineIntakeMg,
        int dailyCaffeineIntakeRate,
        String intakeGuide,
        float sleepSensitiveThreshold,
        List<HourlyCaffeineInfo> caffeineByHour
) {
    public record HourlyCaffeineInfo(
            String time,
            float caffeineMg
    ) {}
}
