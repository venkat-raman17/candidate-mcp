package com.example.mcpserver.dto.common.enums;

/**
 * Status of an ApplicationGroup (draft multi-job application).
 */
public enum ApplicationGroupStatus {
    /**
     * Application in progress, not yet submitted
     */
    DRAFT,

    /**
     * Application submitted to all selected jobs
     */
    SUBMITTED,

    /**
     * Application started but abandoned by candidate
     */
    ABANDONED
}
