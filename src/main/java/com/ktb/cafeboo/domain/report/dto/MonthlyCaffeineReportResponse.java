package com.ktb.cafeboo.domain.report.dto;

import java.util.List;

public record MonthlyCaffeineReportResponse(
        Filter filter,
        String startDate,
        String endDate,
        float monthlyCaffeineTotal,
        float weeklyCaffeineAvg,
        List<WeeklyIntakeTotal> weeklyIntakeTotals
) {
    public record Filter(
            String year,
            String month
    ) {}

    public record WeeklyIntakeTotal(
            String isoWeek,
            float totalCaffeineMg
    ) {}
}