package com.example.notes.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Component;

/**
 * In-memory, single-instance counters that back the operations dashboard: total traffic,
 * status-code class (2xx/3xx/4xx/5xx), error-code breakdown, and a rolling per-minute traffic
 * series. This is intentionally lightweight (no external metrics backend) to match the rest of
 * this demo: counters live in process memory and reset on restart or redeploy, exactly like the
 * in-memory H2 database used for the public demo profile.
 */
@Component
public class RequestMetricsRecorder {
    private static final int RETAINED_MINUTES = 60;

    private final Instant startedAt = Instant.now();
    private final LongAdder totalRequests = new LongAdder();
    private final Map<StatusClass, LongAdder> byStatusClass = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> byErrorCode = new ConcurrentHashMap<>();
    private final Map<Long, LongAdder> requestsByMinute = new ConcurrentHashMap<>();

    public void recordRequest(int statusCode) {
        totalRequests.increment();
        byStatusClass.computeIfAbsent(StatusClass.of(statusCode), ignored -> new LongAdder()).increment();

        long minuteKey = Instant.now().getEpochSecond() / 60;
        requestsByMinute.computeIfAbsent(minuteKey, ignored -> new LongAdder()).increment();
        requestsByMinute.keySet().removeIf(minute -> minuteKey - minute >= RETAINED_MINUTES);
    }

    public void recordErrorCode(String errorCode) {
        byErrorCode.computeIfAbsent(errorCode, ignored -> new LongAdder()).increment();
    }

    public MetricsSnapshot snapshot() {
        Instant now = Instant.now();
        long currentMinute = now.getEpochSecond() / 60;

        List<MetricsSnapshot.MinutePoint> traffic = new ArrayList<>(RETAINED_MINUTES);
        for (long minute = currentMinute - (RETAINED_MINUTES - 1); minute <= currentMinute; minute++) {
            LongAdder bucket = requestsByMinute.get(minute);
            traffic.add(new MetricsSnapshot.MinutePoint(
                    Instant.ofEpochSecond(minute * 60), bucket == null ? 0 : bucket.sum()));
        }

        List<MetricsSnapshot.ErrorCodeCount> errorCodes = byErrorCode.entrySet().stream()
                .map(entry -> new MetricsSnapshot.ErrorCodeCount(entry.getKey(), entry.getValue().sum()))
                .sorted(Comparator.comparingLong(MetricsSnapshot.ErrorCodeCount::count).reversed())
                .toList();

        return new MetricsSnapshot(
                startedAt,
                now,
                totalRequests.sum(),
                sum(StatusClass.TWO_XX),
                sum(StatusClass.THREE_XX),
                sum(StatusClass.FOUR_XX),
                sum(StatusClass.FIVE_XX),
                errorCodes,
                traffic);
    }

    private long sum(StatusClass statusClass) {
        LongAdder adder = byStatusClass.get(statusClass);
        return adder == null ? 0 : adder.sum();
    }
}
