package pt.estga.processing.enums;

public enum FanOutStrategy {
    /**
     * Split an evidence's contribution across all associated marks (1/N each).
     */
    SPLIT,
    /**
     * Duplicate full contribution to every associated mark (may inflate scores).
     */
    FULL
}