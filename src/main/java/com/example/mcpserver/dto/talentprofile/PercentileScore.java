package com.example.mcpserver.dto.talentprofile;

import com.example.mcpserver.model.AssessmentType;

/**
 * Aggregated percentile score for an assessment type.
 */
public record PercentileScore(
        AssessmentType type,
        Double averagePercentile,
        Integer sampleSize
) {
}
