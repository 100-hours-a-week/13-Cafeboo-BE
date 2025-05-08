package com.ktb.cafeboo.domain.report.dto;

import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse.DailyIntakeTotal;
import com.ktb.cafeboo.domain.report.dto.WeeklyCaffeineReportResponse.Filter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
public class MonthlyCaffeineReportResponse {
    private MonthlyCaffeineReportResponse.Filter filter;
    private String startDate;
    private String endDate;
    private float monthlyCaffeineTotal;
    private float weeklyCaffeineAvg;
    private List<MonthlyCaffeineReportResponse.weeklyIntakeTotal> weeklyIntakeTotals;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Filter {
        private String year;
        private String month;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class weeklyIntakeTotal {
        private String isoWeek;
        private float totalCaffeineMg;
    }
}
