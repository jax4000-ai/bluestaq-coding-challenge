package com.example.notes.observability;

/**
 * HTTP status buckets used by the operations dashboard. Anything outside the
 * standard 2xx/3xx/4xx/5xx ranges (which should not happen in practice) is grouped as OTHER
 * so the dashboard total always reconciles with the sum of its buckets.
 */
enum StatusClass {
    TWO_XX,
    THREE_XX,
    FOUR_XX,
    FIVE_XX,
    OTHER;

    static StatusClass of(int statusCode) {
        return switch (statusCode / 100) {
            case 2 -> TWO_XX;
            case 3 -> THREE_XX;
            case 4 -> FOUR_XX;
            case 5 -> FIVE_XX;
            default -> OTHER;
        };
    }
}
