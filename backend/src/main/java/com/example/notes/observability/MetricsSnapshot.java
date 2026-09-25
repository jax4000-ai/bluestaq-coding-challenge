package com.example.notes.observability;

import java.time.Instant;
import java.util.List;

/** Aggregate, non-sensitive operational counters returned by the dashboard endpoint. */
public record MetricsSnapshot(
        Instant startedAt,
        Instant generatedAt,
        long totalRequests,
        long twoXx,
        long threeXx,
        long fourXx,
        long fiveXx,
        List<ErrorCodeCount> errorCodes,
        List<MinutePoint> trafficByMinute) {

    public record ErrorCodeCount(String code, long count) {
    }

    public record MinutePoint(Instant minute, long count) {
    }
}
