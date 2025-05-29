package com.ktb.cafeboo.domain.report.dto;

import java.time.LocalTime;

public record CoffeeTimeStats(
    LocalTime firstAvg,
    LocalTime lastAvg,
    int lateNightDays
) {}
