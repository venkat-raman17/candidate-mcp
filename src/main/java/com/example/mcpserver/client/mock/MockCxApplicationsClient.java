package com.example.mcpserver.client.mock;

import com.example.mcpserver.client.CxApplicationsClient;
import com.example.mcpserver.dto.cxapplications.ApplicationGroup;
import com.example.mcpserver.dto.cxapplications.AtsApplication;
import com.example.mcpserver.store.CxApplicationsMockStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Mock implementation of CxApplicationsClient using comprehensive in-memory data store.
 * Contains 7 realistic applications and 3 application groups across multiple candidates.
 *
 * Test Scenarios:
 * - C001: 3 applications (1 technical interview, 1 offer extended, 1 rejected)
 * - C002: 1 application (screening stage) + 1 draft application group
 * - C003: 1 application (hired)
 * - C004: 1 application (SLA breach scenario)
 * - C005: 1 application (withdrawn)
 */
@Component
@Profile("!production")
public class MockCxApplicationsClient implements CxApplicationsClient {

    @Override
    public Optional<ApplicationGroup> getApplicationGroup(String groupId) {
        return CxApplicationsMockStore.getApplicationGroup(groupId);
    }

    @Override
    public List<ApplicationGroup> getApplicationGroupsByCandidate(String candidateId) {
        return CxApplicationsMockStore.getApplicationGroupsByCandidate(candidateId);
    }

    @Override
    public Optional<AtsApplication> getApplication(String applicationId) {
        return CxApplicationsMockStore.getApplication(applicationId);
    }

    @Override
    public List<AtsApplication> getApplicationsByCandidate(String candidateId) {
        return CxApplicationsMockStore.getApplicationsByCandidate(candidateId);
    }

    @Override
    public List<AtsApplication> getApplicationsByJob(String jobId) {
        return CxApplicationsMockStore.getApplicationsByJob(jobId);
    }

    @Override
    public Optional<AtsApplication> getApplicationByCandidateAndJob(String candidateId, String jobId) {
        return CxApplicationsMockStore.getApplicationByCandidateAndJob(candidateId, jobId);
    }
}
