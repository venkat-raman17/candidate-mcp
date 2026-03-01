package com.example.mcpserver.dto.talentprofile;

import java.util.List;
import java.util.Map;

/**
 * Contains all assessment results and aggregated statistics.
 */
public record AssessmentResults(
        List<AssessmentResult> results,
        Map<String, PercentileScore> percentilesByType
) {
}
