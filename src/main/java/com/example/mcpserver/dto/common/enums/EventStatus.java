package com.example.mcpserver.dto.common.enums;

/**
 * Status of a scheduled interview or event.
 */
public enum EventStatus {
    /**
     * Event scheduled, not yet occurred
     */
    SCHEDULED,

    /**
     * Event completed successfully
     */
    COMPLETED,

    /**
     * Event cancelled before it occurred
     */
    CANCELLED,

    /**
     * Event rescheduled to a different time
     */
    RESCHEDULED,

    /**
     * Candidate did not show up for the event
     */
    NO_SHOW,

    /**
     * Interviewer did not show up
     */
    INTERVIEWER_NO_SHOW
}
