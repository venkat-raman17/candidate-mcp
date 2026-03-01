package com.example.mcpserver.dto.cxapplications;

import com.example.mcpserver.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tracks application status transitions through the hiring workflow.
 */
public record WorkflowHistoryEntry(
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        LocalDateTime transitionedAt,
        String transitionedBy,
        String transitionedByName,
        String reason,
        String notes,
        Map<String, Object> metadata
) {
}
