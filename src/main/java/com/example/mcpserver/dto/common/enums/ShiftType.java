package com.example.mcpserver.dto.common.enums;

/**
 * Shift types for job requisitions and candidate preferences.
 * Used by both job-sync-service (requirements) and talent-profile-service (preferences).
 */
public enum ShiftType {
    /**
     * Standard daytime hours (typically 08:00-17:00 or 09:00-18:00)
     */
    DAY,

    /**
     * Overnight hours (typically 22:00-06:00 or 23:00-07:00)
     */
    NIGHT,

    /**
     * Swing shift (typically 15:00-23:00 or 16:00-00:00)
     */
    SWING,

    /**
     * Rotating schedule (shifts change weekly or bi-weekly)
     */
    ROTATING,

    /**
     * Flexible hours within business days
     */
    FLEXIBLE,

    /**
     * Split shifts (e.g., morning + evening with break in between)
     */
    SPLIT
}
