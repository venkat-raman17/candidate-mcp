package com.example.mcpserver.model;

import java.time.LocalDateTime;
import java.util.Map;

public record AssessmentResult(
        String id,
        String candidateId,
        String applicationId,
        AssessmentType type,
        double score,
        double maxScore,
        int percentile,
        LocalDateTime completedAt,
        String summary,
        Map<String, Object> breakdown
) {
    public double scorePercent() {
        return maxScore > 0 ? (score / maxScore) * 100 : 0;
    }
}
