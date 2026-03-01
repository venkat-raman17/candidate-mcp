package com.example.mcpserver.dto.common.enums;

/**
 * Types of scheduled events in the application workflow.
 */
public enum EventType {
    /**
     * Initial phone screening with recruiter
     */
    PHONE_SCREEN,

    /**
     * Technical interview (coding, system design, etc.)
     */
    TECH_INTERVIEW,

    /**
     * Full-day onsite interview loop
     */
    ONSITE,

    /**
     * Final round with hiring manager or leadership
     */
    FINAL_ROUND,

    /**
     * Offer discussion call
     */
    OFFER_CALL,

    /**
     * Post-acceptance orientation or onboarding session
     */
    ORIENTATION,

    /**
     * Panel interview with multiple interviewers
     */
    PANEL_INTERVIEW,

    /**
     * Informal coffee chat or culture fit conversation
     */
    INFORMAL_CHAT
}
