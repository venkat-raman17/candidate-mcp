package com.example.mcpserver.dto.common.enums;

/**
 * Work location mode for jobs and candidate preferences.
 */
public enum WorkMode {
    /**
     * Fully remote - no onsite requirement
     */
    REMOTE,

    /**
     * Hybrid - mix of remote and onsite (X days per week)
     */
    HYBRID,

    /**
     * Fully onsite - must be in office every day
     */
    ONSITE,

    /**
     * Flexible - candidate/employee chooses their preferred mode
     */
    FLEXIBLE
}
