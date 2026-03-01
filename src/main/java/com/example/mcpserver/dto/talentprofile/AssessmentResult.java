package com.example.mcpserver.dto.talentprofile;

import com.example.mcpserver.model.AssessmentType;

import java.time.LocalDateTime;

/**
 * Result of a single assessment taken by the candidate.
 */
public record AssessmentResult(
        String assessmentId,
        String assessmentCode,
        AssessmentType type,
        Integer score,
        Integer maxScore,
        Integer percentile,
        LocalDateTime completedAt,
        Boolean passed,
        String summary
) {
}
