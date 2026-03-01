package com.example.mcpserver.transformer;

import com.example.mcpserver.dto.agentcontext.ApplicationAgentContext;
import com.example.mcpserver.dto.agentcontext.OfferSummary;
import com.example.mcpserver.dto.agentcontext.PublicRecruiterNote;
import com.example.mcpserver.dto.agentcontext.ScheduledEventSummary;
import com.example.mcpserver.dto.agentcontext.WorkflowStageSummary;
import com.example.mcpserver.dto.cxapplications.AtsApplication;
import com.example.mcpserver.dto.cxapplications.CompensationOffer;
import com.example.mcpserver.dto.cxapplications.OfferMetadata;
import com.example.mcpserver.dto.cxapplications.RecruiterNote;
import com.example.mcpserver.dto.cxapplications.ScheduleMetadata;
import com.example.mcpserver.dto.cxapplications.ScheduledEvent;
import com.example.mcpserver.dto.cxapplications.WorkflowHistoryEntry;
import com.example.mcpserver.dto.common.enums.EventStatus;
import com.example.mcpserver.dto.common.enums.OfferStatus;
import com.example.mcpserver.model.ApplicationStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transformer that converts AtsApplication into agent-safe ApplicationAgentContext.
 * <p>
 * This transformer implements Layer 1 data processing with the following operations:
 * </p>
 * <h3>Removed Fields (PII and Internal Data)</h3>
 * <ul>
 *   <li>{@code assignedRecruiterId} - Internal staff ID</li>
 *   <li>{@code internalRating} - Private candidate evaluation score</li>
 *   <li>{@code offerLetterUrl} - Document links with PII</li>
 *   <li>{@code _cosmosPartitionKey} - Database implementation detail</li>
 *   <li>{@code _etag} - Concurrency control metadata</li>
 *   <li>Workflow transition IDs (transitionedBy) - Internal staff IDs</li>
 *   <li>Interviewer IDs - Replaced with names only</li>
 *   <li>Offer negotiation internal notes - Stripped from negotiation rounds</li>
 *   <li>Recruiter note author IDs - Removed, keeping only content and timestamp</li>
 * </ul>
 *
 * <h3>Derived Fields (Computed for Agent Use)</h3>
 * <ul>
 *   <li>{@code currentStage} - Current application status</li>
 *   <li>{@code daysInCurrentStage} - Duration in current stage for SLA tracking</li>
 *   <li>{@code slaBreached} - Whether current stage has exceeded SLA threshold</li>
 *   <li>{@code workflowSummary} - Simplified workflow history without internal IDs</li>
 *   <li>{@code scheduledEvents} - Interview schedule with interviewer names only</li>
 *   <li>{@code offerSummary} - Public offer information without internal notes</li>
 * </ul>
 *
 * <h3>SLA Thresholds (Days)</h3>
 * <ul>
 *   <li>RECEIVED: 2 days</li>
 *   <li>SCREENING: 5 days</li>
 *   <li>PHONE_INTERVIEW: 3 days</li>
 *   <li>TECHNICAL_INTERVIEW: 7 days</li>
 *   <li>FINAL_INTERVIEW: 5 days</li>
 *   <li>OFFER_EXTENDED: 5 days</li>
 * </ul>
 *
 * @see AtsApplication
 * @see ApplicationAgentContext
 * @since 1.0
 */
@Component
public class ApplicationTransformer implements AgentContextTransformer<AtsApplication, ApplicationAgentContext> {

    /**
     * SLA thresholds in days for each application status.
     * Applications exceeding these thresholds are flagged as SLA breached.
     */
    private static final Map<ApplicationStatus, Integer> SLA_DAYS = Map.of(
            ApplicationStatus.RECEIVED, 2,
            ApplicationStatus.SCREENING, 5,
            ApplicationStatus.PHONE_INTERVIEW, 3,
            ApplicationStatus.TECHNICAL_INTERVIEW, 7,
            ApplicationStatus.FINAL_INTERVIEW, 5,
            ApplicationStatus.OFFER_EXTENDED, 5
    );

    /**
     * Transforms an ATS application into an agent-safe context.
     *
     * @param source the raw ATS application from CxApplications system
     * @return agent-safe application context, or null if source is null
     */
    @Override
    public ApplicationAgentContext transform(AtsApplication source) {
        if (source == null) {
            return null;
        }

        ApplicationStatus currentStage = source.status();
        int daysInStage = calculateDaysInCurrentStage(source);
        boolean slaBreached = isSlaBreached(currentStage, daysInStage);

        return new ApplicationAgentContext(
                source.applicationId(),
                source.candidateId(),
                source.jobId(),
                currentStage,
                source.source(),
                source.appliedAt(),
                currentStage.name(),
                daysInStage,
                slaBreached,
                transformWorkflowHistory(source.workflowHistory()),
                transformScheduledEvents(source.schedule()),
                transformOfferMetadata(source.offer()),
                transformRecruiterNotes(source.notes())
        );
    }

    /**
     * Calculates the number of days the application has been in its current stage.
     *
     * @param application the ATS application
     * @return days in current stage
     */
    private int calculateDaysInCurrentStage(AtsApplication application) {
        if (application.workflowHistory() == null || application.workflowHistory().isEmpty()) {
            // If no workflow history, use time since submission
            return (int) Duration.between(application.appliedAt(), Instant.now()).toDays();
        }

        // Find the most recent workflow entry
        WorkflowHistoryEntry latestEntry = application.workflowHistory()
                .get(application.workflowHistory().size() - 1);

        return (int) Duration.between(latestEntry.transitionedAt(), Instant.now()).toDays();
    }

    /**
     * Determines if the application has breached SLA for its current stage.
     *
     * @param currentStatus the current application status
     * @param daysInStage the number of days in current stage
     * @return true if SLA is breached, false otherwise
     */
    private boolean isSlaBreached(ApplicationStatus currentStatus, int daysInStage) {
        Integer slaThreshold = SLA_DAYS.get(currentStatus);
        if (slaThreshold == null) {
            // No SLA defined for this status (e.g., HIRED, REJECTED, WITHDRAWN)
            return false;
        }
        return daysInStage > slaThreshold;
    }

    /**
     * Transforms workflow history by removing internal staff IDs.
     *
     * @param workflowHistory the raw workflow history
     * @return list of workflow stage summaries without PII
     */
    private List<WorkflowStageSummary> transformWorkflowHistory(List<WorkflowHistoryEntry> workflowHistory) {
        if (workflowHistory == null) {
            return List.of();
        }

        return workflowHistory.stream()
                .map(entry -> {
                    int daysInStage = (int) Duration.between(entry.transitionedAt(), Instant.now()).toDays();
                    return new WorkflowStageSummary(
                            entry.toStatus().name(),
                            entry.transitionedAt(),
                            daysInStage
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Transforms scheduled events by removing interviewer IDs and keeping only names.
     *
     * @param scheduleMetadata the schedule metadata containing events
     * @return list of scheduled event summaries with interviewer names only
     */
    private List<ScheduledEventSummary> transformScheduledEvents(ScheduleMetadata scheduleMetadata) {
        if (scheduleMetadata == null || scheduleMetadata.events() == null) {
            return List.of();
        }

        return scheduleMetadata.events().stream()
                .map(event -> new ScheduledEventSummary(
                        event.type(),
                        event.scheduledAt(),
                        event.durationMinutes(),
                        event.location(),
                        event.interviewerNames(),
                        event.status()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Transforms offer metadata by removing internal negotiation notes.
     *
     * @param offerMetadata the raw offer metadata
     * @return offer summary without internal notes, or null if no offer
     */
    private OfferSummary transformOfferMetadata(OfferMetadata offerMetadata) {
        if (offerMetadata == null) {
            return null;
        }

        CompensationOffer compensation = offerMetadata.compensation();
        String salaryDisplay = null;
        java.time.LocalDate startDate = null;

        if (compensation != null) {
            salaryDisplay = String.format("$%,d %s",
                    compensation.baseSalary(),
                    compensation.currency());
            if (compensation.startDate() != null) {
                try {
                    startDate = java.time.LocalDate.parse(compensation.startDate());
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }

        return new OfferSummary(
                offerMetadata.offerExtendedAt(),
                offerMetadata.offerExpiresAt(),
                offerMetadata.offerStatus(),
                salaryDisplay,
                startDate
        );
    }

    /**
     * Transforms recruiter notes by removing author IDs.
     *
     * @param recruiterNotes the raw recruiter notes
     * @return list of public recruiter notes without author IDs
     */
    private List<PublicRecruiterNote> transformRecruiterNotes(List<RecruiterNote> recruiterNotes) {
        if (recruiterNotes == null) {
            return List.of();
        }

        return recruiterNotes.stream()
                .map(note -> new PublicRecruiterNote(
                        note.createdAt(),
                        note.note(),
                        note.authorName()
                ))
                .collect(Collectors.toList());
    }
}
