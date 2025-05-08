package com.ktb.cafeboo.domain.report.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeeklyCaffeineReportResponse {
    private Filter filter;
    private String isoWeek;
    private String startDate;
    private String endDate;
    private float weeklyCaffeineTotal;
    private int dailyCaffeineLimit;
    private int overLimitDays;
    private float dailyCaffeineAvg;
    private List<DailyIntakeTotal> dailyIntakeTotals;
    private String aiMessage;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Filter {
        private String year;
        private String month;
        private String week;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyIntakeTotal {
        private String date;
        private int caffeineMg;
    }
}
