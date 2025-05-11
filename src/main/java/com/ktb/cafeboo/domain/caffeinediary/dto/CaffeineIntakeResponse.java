package com.ktb.cafeboo.domain.caffeinediary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaffeineIntakeResponse {
    private String id;
    private String drinkId;
    private String drinkName;
    private LocalDateTime intakeTime;
    private Integer drinkCount;
    private Float caffeineAmount;
}
