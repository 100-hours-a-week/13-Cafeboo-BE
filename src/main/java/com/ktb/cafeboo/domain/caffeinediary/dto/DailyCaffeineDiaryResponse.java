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
public class DailyCaffeineDiaryResponse {
    private Filter filter;
    private float totalCaffeineMg;
    private List<IntakeDetail> intakeList;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Filter {
        private String date; // yyyy-MM-dd
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IntakeDetail {
        private String intakeId;
        private String drinkId;
        private String drinkName;
        private int drinkCount;
        private float caffeineMg;
        private String intakeTime; // ISO 8601 (LocalDateTime.toString())
    }
}
