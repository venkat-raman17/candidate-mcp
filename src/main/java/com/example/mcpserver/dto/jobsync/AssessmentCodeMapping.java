package com.example.mcpserver.dto.jobsync;

import java.util.List;
import java.util.Map;

/**
 * Assessment requirements for a job requisition.
 * Maps to standardized assessment codes (e.g., JAVA_01, SYS_DESIGN_02).
 */
public record AssessmentCodeMapping(
        List<String> requiredCodes,
        Map<String, String> codeDescriptions,
        Boolean allowExternalCerts
) {
    public static AssessmentCodeMapping empty() {
        return new AssessmentCodeMapping(List.of(), Map.of(), false);
    }
}
