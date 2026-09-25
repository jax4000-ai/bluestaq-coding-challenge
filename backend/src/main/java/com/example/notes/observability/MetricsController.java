package com.example.notes.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated read of aggregate traffic/error counters for this instance. No note
 * content, titles, team names, or identities are exposed here — only counts — so this endpoint
 * intentionally requires no clearance headers, unlike the note and audit APIs.
 */
@RestController
@RequestMapping("/api/observability")
public class MetricsController {
    private final RequestMetricsRecorder recorder;

    public MetricsController(RequestMetricsRecorder recorder) {
        this.recorder = recorder;
    }

    @GetMapping("/metrics")
    public MetricsSnapshot metrics() {
        return recorder.snapshot();
    }
}
