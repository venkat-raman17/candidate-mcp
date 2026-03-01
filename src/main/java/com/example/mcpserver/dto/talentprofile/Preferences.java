package com.example.mcpserver.dto.talentprofile;

/**
 * Aggregated preferences for job matching.
 */
public record Preferences(
        LocationPreferences location,
        JobPreferences job,
        CompensationExpectations compensation,
        WorkStylePreferences workStyle
) {
}
