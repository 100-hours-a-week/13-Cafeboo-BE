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
    private Long id;
    private Long drinkId;
    private String drinkName;
    private LocalDateTime intakeTime;
    private Integer drinkCount;
    private Float caffeineAmount;
}
