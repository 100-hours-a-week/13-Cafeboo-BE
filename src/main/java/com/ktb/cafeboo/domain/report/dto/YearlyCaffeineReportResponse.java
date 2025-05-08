package com.ktb.cafeboo.domain.report.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
public class YearlyCaffeineReportResponse {

    private Filter filter;
    private String startDate;
    private String endDate;
    private float yearlyCaffeineTotal;
    private float monthlyCaffeineAvg;
    private List<MonthlyIntakeTotal> monthlyIntakeTotals;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Filter {
        private String year;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MonthlyIntakeTotal {
        private int month;           // 1~12
        private float totalCaffeineMg;
    }
}
