package com.example.mcpserver.client;

import com.example.mcpserver.dto.cxapplications.ApplicationGroup;
import com.example.mcpserver.dto.cxapplications.AtsApplication;
import java.util.List;
import java.util.Optional;

/**
 * Client interface for accessing candidate application data from the CxApplications system.
 * <p>
 * This interface provides access to two types of application entities:
 * <ul>
 *   <li><strong>ApplicationGroup</strong>: Draft multi-job applications initiated through
 *       candidate portals but not yet submitted to ATS</li>
 *   <li><strong>AtsApplication</strong>: Active or closed applications in the ATS workflow
 *       with full tracking and status management</li>
 * </ul>
 * </p>
 *
 * @see ApplicationGroup
 * @see AtsApplication
 * @since 1.0
 */
public interface CxApplicationsClient {

    // ========== ApplicationGroup methods (draft multi-job applications) ==========

    /**
     * Retrieves a draft application group by its unique identifier.
     * <p>
     * Application groups represent multi-job applications that are in progress
     * through candidate portals (web, mobile, LinkedIn) but have not yet been
     * submitted to the ATS.
     * </p>
     *
     * @param groupId the unique identifier of the application group (e.g., "AG001")
     * @return an Optional containing the application group if found, or empty if not found
     * @throws IllegalArgumentException if groupId is null or empty
     */
    Optional<ApplicationGroup> getApplicationGroup(String groupId);

    /**
     * Retrieves all application groups for a specific candidate.
     * <p>
     * This includes groups in any status (DRAFT, SUBMITTED, ABANDONED) to support
     * candidate experience tracking and conversion analysis.
     * </p>
     *
     * @param candidateId the unique identifier of the candidate (e.g., "C001")
     * @return a list of application groups for the candidate, empty list if none found
     * @throws IllegalArgumentException if candidateId is null or empty
     */
    List<ApplicationGroup> getApplicationGroupsByCandidate(String candidateId);

    // ========== AtsApplication methods (active/closed applications) ==========

    /**
     * Retrieves a single ATS application by its unique identifier.
     *
     * @param applicationId the unique identifier of the application (e.g., "A001")
     * @return an Optional containing the application if found, or empty if not found
     * @throws IllegalArgumentException if applicationId is null or empty
     */
    Optional<AtsApplication> getApplication(String applicationId);

    /**
     * Retrieves all ATS applications for a specific candidate.
     * <p>
     * This includes applications in any status (active, rejected, hired, withdrawn)
     * to provide a complete candidate journey view.
     * </p>
     *
     * @param candidateId the unique identifier of the candidate (e.g., "C001")
     * @return a list of applications for the candidate, empty list if none found
     * @throws IllegalArgumentException if candidateId is null or empty
     */
    List<AtsApplication> getApplicationsByCandidate(String candidateId);

    /**
     * Retrieves all ATS applications for a specific job requisition.
     * <p>
     * This supports job-level analytics such as conversion rates, time-to-hire,
     * and pipeline health.
     * </p>
     *
     * @param jobId the unique identifier of the job (e.g., "J001")
     * @return a list of applications for the job, empty list if none found
     * @throws IllegalArgumentException if jobId is null or empty
     */
    List<AtsApplication> getApplicationsByJob(String jobId);

    /**
     * Retrieves a specific application for a candidate-job combination.
     * <p>
     * This is useful for checking if a candidate has already applied to a job
     * and retrieving the current status of that application.
     * </p>
     *
     * @param candidateId the unique identifier of the candidate (e.g., "C001")
     * @param jobId the unique identifier of the job (e.g., "J001")
     * @return an Optional containing the application if found, or empty if the
     *         candidate has not applied to this job
     * @throws IllegalArgumentException if candidateId or jobId is null or empty
     */
    Optional<AtsApplication> getApplicationByCandidateAndJob(String candidateId, String jobId);
}
