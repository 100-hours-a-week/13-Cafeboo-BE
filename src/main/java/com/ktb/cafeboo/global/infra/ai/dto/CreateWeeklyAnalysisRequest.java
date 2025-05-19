package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateWeeklyAnalysisRequest {

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("users")
    private List<UserReportData> users;

    @Getter
    @Setter
    @Builder
    public static class UserReportData {
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("data")
        private Data data;
    }

    @Getter
    @Setter
    @Builder
    public static class Data {
        @JsonProperty("period")
        private String period;

        @JsonProperty("avg_caffeine_per_day")
        private float avgCaffeinePerDay;

        @JsonProperty("recommended_daily_limit")
        private float recommendedDailyLimit;

        @JsonProperty("percentage_of_limit")
        private float percentageOfLimit;

        @JsonProperty("highlight_day_high")
        private String highlightDayHigh;

        @JsonProperty("highlight_day_low")
        private String highlightDayLow;

        @JsonProperty("first_coffee_avg")
        private String firstCoffeeAvg;

        @JsonProperty("last_coffee_avg")
        private String lastCoffeeAvg;

        @JsonProperty("late_night_caffeine_days")
        private Integer lateNightCaffeineDays;

        @JsonProperty("over_100mg_before_sleep_days")
        private Integer over100mgBeforeSleepDays;

        @JsonProperty("average_sleep_quality")
        private String averageSleepQuality;
    }
}