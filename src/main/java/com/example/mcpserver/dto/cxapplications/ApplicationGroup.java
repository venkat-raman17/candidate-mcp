package com.example.mcpserver.dto.cxapplications;

import com.example.mcpserver.dto.common.enums.ApplicationGroupStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a draft multi-job application group.
 * Allows candidates to apply to multiple jobs simultaneously.
 */
public record ApplicationGroup(
        String groupId,
        String candidateId,
        List<String> jobIds,
        ApplicationGroupStatus status,
        String sourcePlatform,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        Map<String, Object> draftData,
        Integer completionPercentage
) {
}
