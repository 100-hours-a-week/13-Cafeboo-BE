package com.ktb.cafeboo.domain.caffeinediary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaffeineIntakeRequest {
    @JsonProperty("drinkId")
    private Long drinkId;

    @JsonProperty("intakeTime")
    private LocalDateTime intakeTime;

    @JsonProperty("drinkCount")
    private Integer drinkCount;

    @JsonProperty("caffeineAmount")
    private Float caffeineAmount;

    @JsonProperty("drinkSize")
    private String drinkSize;
}
