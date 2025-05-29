package com.ktb.cafeboo.domain.report.dto;

import java.util.List;

public record WeeklyCaffeineReportResponse(
        Filter filter,
        String isoWeek,
        String startDate,
        String endDate,
        float weeklyCaffeineTotal,
        int dailyCaffeineLimit,
        int overLimitDays,
        float dailyCaffeineAvg,
        List<DailyIntakeTotal> dailyIntakeTotals,
        String aiMessage
) {
    public record Filter(
            String year,
            String month,
            String week
    ) {}

    public record DailyIntakeTotal(
            String date,
            int caffeineMg
    ) {}
}