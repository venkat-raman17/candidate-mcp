package com.example.mcpserver.client.mock;

import com.example.mcpserver.client.JobSyncClient;
import com.example.mcpserver.dto.jobsync.JobRequisitionDocument;
import com.example.mcpserver.store.JobSyncMockStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mock implementation of JobSyncClient using comprehensive in-memory data store.
 * Contains 8 realistic job requisitions across various departments and statuses.
 */
@Component
@Profile("!production")
public class MockJobSyncClient implements JobSyncClient {

    @Override
    public Optional<JobRequisitionDocument> getJob(String jobId) {
        return JobSyncMockStore.getJob(jobId);
    }

    @Override
    public List<JobRequisitionDocument> getActiveJobs() {
        return JobSyncMockStore.getActiveJobs();
    }

    @Override
    public List<JobRequisitionDocument> getJobsByDepartment(String department) {
        return JobSyncMockStore.getJobsByDepartment(department);
    }
}
