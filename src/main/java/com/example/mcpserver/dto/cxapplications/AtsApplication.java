package com.example.mcpserver.dto.cxapplications;

import com.example.mcpserver.model.ApplicationSource;
import com.example.mcpserver.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full Cosmos document representing an application in the ATS.
 * Contains workflow history, scheduling, offers, and internal notes.
 */
public record AtsApplication(
        String applicationId,
        String candidateId,
        String jobId,
        ApplicationStatus status,
        ApplicationSource source,
        LocalDateTime appliedAt,
        LocalDateTime lastUpdatedAt,
        List<WorkflowHistoryEntry> workflowHistory,
        ScheduleMetadata schedule,
        OfferMetadata offer,
        List<RecruiterNote> notes,
        String assignedRecruiterId,
        String internalRating,
        String _cosmosPartitionKey,
        String _etag
) {
}
