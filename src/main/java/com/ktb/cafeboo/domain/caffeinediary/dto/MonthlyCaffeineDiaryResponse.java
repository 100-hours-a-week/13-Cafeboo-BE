package com.ktb.cafeboo.domain.caffeinediary.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyCaffeineDiaryResponse {
    private Filter filter;
    private List<DailyIntake> dailyIntakeList;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Filter {
        private String year;
        private String month;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyIntake {
        private String date; // yyyy-MM-dd
        private float totalCaffeineMg;
    }
}
