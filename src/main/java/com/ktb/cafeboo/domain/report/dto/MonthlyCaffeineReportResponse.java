package com.ktb.cafeboo.domain.report.dto;

import com.ktb.cafeboo.domain.report.model.WeeklyReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record MonthlyCaffeineReportResponse(
        Filter filter,
        String startDate,
        String endDate,
        float monthlyCaffeineTotal,
        float weeklyCaffeineAvg,
        List<WeeklyIntake> weeklyIntakeTotals
) {
    public record Filter(
            String year,
            String month
    ) {}

    public record WeeklyIntake(
            String isoWeek,
            float totalCaffeineMg
    ) {}

    public static MonthlyCaffeineReportResponse create(
        int resolvedYear,
        int resolvedMonth,
        LocalDate startDate,
        LocalDate endDate,
        Map<Integer, WeeklyReport> reportMap,
        Set<Integer> weekNums
    ) {
        List<WeeklyIntake> weeklyIntakeTotals = weekNums.stream()
            .map(weekNum -> {
                WeeklyReport report = reportMap.get(weekNum);
                if (report != null) {
                    return new WeeklyIntake(
                        String.format("%d-W%02d", report.getYear(), report.getWeekNum()),
                        Math.round(report.getTotalCaffeineMg())
                    );
                } else {
                    return new WeeklyIntake(
                        String.format("%d-W%02d", resolvedYear, weekNum),
                        0L
                    );
                }
            })
            .collect(Collectors.toList());

        float sum = (float) weeklyIntakeTotals.stream()
            .mapToDouble(WeeklyIntake::totalCaffeineMg)
            .sum();

        float avg = weeklyIntakeTotals.isEmpty() ? 0f : sum / weeklyIntakeTotals.size();

        return new MonthlyCaffeineReportResponse(
            new Filter(String.valueOf(resolvedYear), String.valueOf(resolvedMonth)),
            startDate.toString(),
            endDate.toString(),
            sum,
            avg,
            weeklyIntakeTotals
        );
    }
}