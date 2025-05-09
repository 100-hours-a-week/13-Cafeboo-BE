package com.ktb.cafeboo.domain.report.dto;

import java.time.LocalTime;

public class CoffeeTimeStats {
    public final LocalTime firstAvg;
    public final LocalTime lastAvg;
    public final int lateNightDays;

    public CoffeeTimeStats(LocalTime firstAvg, LocalTime lastAvg, int lateNightDays) {
        this.firstAvg = firstAvg;
        this.lastAvg = lastAvg;
        this.lateNightDays = lateNightDays;
    }
}
