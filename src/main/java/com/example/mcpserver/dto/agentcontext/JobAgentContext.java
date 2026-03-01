package com.example.mcpserver.dto.agentcontext;

import com.example.mcpserver.dto.jobsync.CompensationDetails;
import com.example.mcpserver.dto.jobsync.RequirementSection;
import com.example.mcpserver.dto.jobsync.ShiftDetails;
import com.example.mcpserver.model.JobStatus;
import com.example.mcpserver.model.JobType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Layer 1 projection of JobRequisitionDocument.
 * PII-stripped version with computed fields for agent consumption.
 * Excludes: costCenter, budgetCode, internalNotes, Cosmos metadata.
 */
public record JobAgentContext(
        // Public fields from JobRequisitionDocument
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
        String hiringManagerId,
        String hiringManagerName,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        Integer targetHeadcount,

        // Computed fields for agent context
        String salaryRangeDisplay,
        List<String> requiredAssessmentCodes
) {
}
