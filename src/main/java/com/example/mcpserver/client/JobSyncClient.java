package com.example.mcpserver.client;

import com.example.mcpserver.dto.jobsync.JobRequisitionDocument;
import java.util.List;
import java.util.Optional;

/**
 * Client interface for accessing job requisition data from the JobSync system.
 * <p>
 * This interface defines the contract for retrieving job postings and requisitions
 * from the enterprise job management system. Implementations may be backed by REST APIs,
 * message queues, or mock data stores for testing.
 * </p>
 *
 * @see JobRequisitionDocument
 * @since 1.0
 */
public interface JobSyncClient {

    /**
     * Retrieves a single job requisition by its unique identifier.
     *
     * @param jobId the unique identifier of the job requisition (e.g., "J001")
     * @return an Optional containing the job if found, or empty if not found
     * @throws IllegalArgumentException if jobId is null or empty
     */
    Optional<JobRequisitionDocument> getJob(String jobId);

    /**
     * Retrieves all active job requisitions that are currently open for applications.
     * <p>
     * A job is considered active if its status is OPEN. Jobs with status FILLED,
     * CANCELLED, or DRAFT are excluded.
     * </p>
     *
     * @return a list of active job requisitions, empty list if none found
     */
    List<JobRequisitionDocument> getActiveJobs();

    /**
     * Retrieves all job requisitions for a specific department.
     * <p>
     * This includes jobs in any status (OPEN, FILLED, CANCELLED, DRAFT) to support
     * historical analysis and planning.
     * </p>
     *
     * @param department the department name (e.g., "Engineering", "Sales")
     * @return a list of job requisitions for the department, empty list if none found
     * @throws IllegalArgumentException if department is null or empty
     */
    List<JobRequisitionDocument> getJobsByDepartment(String department);
}
