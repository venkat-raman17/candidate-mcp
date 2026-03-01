package com.example.mcpserver.dto.agentcontext;

import com.example.mcpserver.model.ApplicationSource;
import com.example.mcpserver.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Layer 1 projection of AtsApplication for agent consumption.
 * PII-stripped with computed workflow metrics and summaries.
 */
public record ApplicationAgentContext(
        String applicationId,
        String candidateId,
        String jobId,
        ApplicationStatus status,
        ApplicationSource source,
        LocalDateTime appliedAt,
        String currentStage,
        Integer daysInCurrentStage,
        Boolean slaBreached,
        List<WorkflowStageSummary> workflowSummary,
        List<ScheduledEventSummary> upcomingEvents,
        OfferSummary offerSummary,
        List<PublicRecruiterNote> publicNotes
) {
}
