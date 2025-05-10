package com.ktb.cafeboo.global.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateWeeklyReportRequest {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("period")
    private String period;

    @JsonProperty("avg_caffeine_per_day")
    private Integer avgCaffeinePerDay;

    @JsonProperty("recommended_daily_limit")
    private Integer recommendedDailyLimit;

    @JsonProperty("percentage_of_limit")
    private Integer percentageOfLimit;

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
