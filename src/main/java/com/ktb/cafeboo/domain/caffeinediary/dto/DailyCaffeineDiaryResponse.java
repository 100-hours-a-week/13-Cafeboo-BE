package com.ktb.cafeboo.domain.caffeinediary.dto;

import java.util.List;

public record DailyCaffeineDiaryResponse(
        Filter filter,
        float totalCaffeineMg,
        List<IntakeDetail> intakeList
) {
    public record Filter(String date) {}
    public record IntakeDetail(
            String intakeId,
            String drinkId,
            String drinkName,
            int drinkCount,
            float caffeineMg,
            String intakeTime
    ) {}
}
