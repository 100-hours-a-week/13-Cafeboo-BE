package com.ktb.cafeboo.domain.caffeinediary.dto;

import java.util.List;

public record MonthlyCaffeineDiaryResponse(
        Filter filter,
        List<DailyIntake> dailyIntakeList
) {
    public record Filter(
            String year,
            String month
    ) {}

    public record DailyIntake(
            String date,
            float totalCaffeineMg
    ) {}
}
