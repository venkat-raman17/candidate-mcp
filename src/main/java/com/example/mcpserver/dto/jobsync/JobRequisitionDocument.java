package com.example.mcpserver.dto.jobsync;

import com.example.mcpserver.model.JobStatus;
import com.example.mcpserver.model.JobType;

import java.time.LocalDateTime;

/**
 * Full Cosmos document shape returned by job-sync-service.
 * Contains both public fields (returned to agents) and internal/PII fields (stripped in Layer 1).
 */
public record JobRequisitionDocument(
        // Public fields
        String jobId,
        String requisitionNumber,
        String title,
        String department,
        String location,
        JobType jobType,
        JobStatus status,
        String description,
        RequirementSection requirements,
        CompensationDetails compensation,
        ShiftDetails shift,
        AssessmentCodeMapping assessments,
        String hiringManagerId,
        String hiringManagerName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        Integer targetHeadcount,

        // Internal/PII fields (stripped in Layer 1 transformation)
        String costCenter,
        String budgetCode,
        String internalNotes,

        // Cosmos metadata (stripped)
        String _cosmosPartitionKey,
        String _etag
) {
}
