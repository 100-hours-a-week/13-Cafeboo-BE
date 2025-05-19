package com.ktb.cafeboo.domain.report.dto;

import java.util.List;

public record YearlyCaffeineReportResponse(
        Filter filter,
        String startDate,
        String endDate,
        float yearlyCaffeineTotal,
        float monthlyCaffeineAvg,
        List<MonthlyIntakeTotal> monthlyIntakeTotals
) {
    public record Filter(
            String year
    ) {}

    public record MonthlyIntakeTotal(
            int month,
            float totalCaffeineMg
    ) {}
}
