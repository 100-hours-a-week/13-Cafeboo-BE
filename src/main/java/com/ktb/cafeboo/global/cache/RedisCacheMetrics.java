package com.ktb.cafeboo.global.cache;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RedisCacheMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger hitCount = new AtomicInteger(0);
    private final AtomicInteger missCount = new AtomicInteger(0);

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("chat_message_cache_hit", hitCount, AtomicInteger::get)
                .description("Chat message cache hit count")
                .register(meterRegistry);

        Gauge.builder("chat_message_cache_miss", missCount, AtomicInteger::get)
                .description("Chat message cache miss count")
                .register(meterRegistry);
    }

    public void incrementHit() {
        hitCount.incrementAndGet();
    }

    public void incrementMiss() {
        missCount.incrementAndGet();
    }

    public double getHitRatio() {
        int hits = hitCount.get();
        int misses = missCount.get();
        int total = hits + misses;

        if (total == 0) return 0.0;
        return (double) hits / total;
    }
}
