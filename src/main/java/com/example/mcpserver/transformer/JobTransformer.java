package com.example.mcpserver.transformer;

import com.example.mcpserver.dto.agentcontext.JobAgentContext;
import com.example.mcpserver.dto.jobsync.JobRequisitionDocument;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transformer that converts JobRequisitionDocument into agent-safe JobAgentContext.
 * <p>
 * This transformer implements Layer 1 data processing with the following operations:
 * </p>
 * <h3>Removed Fields (PII and Internal Data)</h3>
 * <ul>
 *   <li>{@code costCenter} - Internal budget tracking</li>
 *   <li>{@code budgetCode} - Internal finance codes</li>
 *   <li>{@code internalNotes} - Recruiter/hiring manager private notes</li>
 *   <li>{@code _cosmosPartitionKey} - Database implementation detail</li>
 *   <li>{@code _etag} - Concurrency control metadata</li>
 * </ul>
 *
 * <h3>Derived Fields (Computed for Agent Use)</h3>
 * <ul>
 *   <li>{@code salaryRangeDisplay} - Formatted salary range from compensation data</li>
 *   <li>{@code requiredAssessmentCodes} - Extracted from assessments.requiredCodes</li>
 * </ul>
 *
 * <h3>Retained Fields</h3>
 * <ul>
 *   <li>Job identification (jobId, title, department)</li>
 *   <li>Public job description and requirements</li>
 *   <li>Location and work arrangement details</li>
 *   <li>Compensation range (public information for candidates)</li>
 *   <li>Shift type and schedule details</li>
 *   <li>Assessment requirements (codes only, no internal metadata)</li>
 *   <li>Job status and posting dates</li>
 * </ul>
 *
 * @see JobRequisitionDocument
 * @see JobAgentContext
 * @since 1.0
 */
@Component
public class JobTransformer implements AgentContextTransformer<JobRequisitionDocument, JobAgentContext> {

    /**
     * Transforms a job requisition into an agent-safe context.
     *
     * @param source the raw job requisition from JobSync system
     * @return agent-safe job context, or null if source is null
     */
    @Override
    public JobAgentContext transform(JobRequisitionDocument source) {
        if (source == null) {
            return null;
        }

        return new JobAgentContext(
                // Public fields from JobRequisitionDocument
                source.jobId(),
                source.requisitionNumber(),
                source.title(),
                source.department(),
                source.location(),
                source.jobType(),
                source.status(),
                source.description(),
                source.requirements(),
                source.compensation(),
                source.shift(),
                source.hiringManagerId(),
                source.hiringManagerName(),
                source.openedAt(),
                source.closedAt(),
                source.targetHeadcount(),

                // Computed fields
                buildSalaryRangeDisplay(source),
                extractRequiredAssessmentCodes(source)

                // STRIPPED: costCenter, budgetCode, internalNotes, _cosmosPartitionKey, _etag
        );
    }

    /**
     * Builds a human-readable salary range display string.
     * <p>
     * Format: "$160,000 - $210,000 USD annually"
     * </p>
     *
     * @param source the job requisition
     * @return formatted salary range, or null if compensation data is missing
     */
    private String buildSalaryRangeDisplay(JobRequisitionDocument source) {
        if (source.compensation() == null) {
            return null;
        }

        var comp = source.compensation();
        return String.format("$%,d - $%,d %s annually",
                comp.salaryRangeMin(),
                comp.salaryRangeMax(),
                comp.currency()
        );
    }

    /**
     * Extracts required assessment codes from the job's assessment requirements.
     * <p>
     * These codes are used to match candidate assessment results and identify
     * skills gaps during candidate evaluation.
     * </p>
     *
     * @param source the job requisition
     * @return list of required assessment codes, or empty list if none specified
     */
    private List<String> extractRequiredAssessmentCodes(JobRequisitionDocument source) {
        if (source.assessments() == null || source.assessments().requiredCodes() == null) {
            return List.of();
        }
        return source.assessments().requiredCodes();
    }
}
