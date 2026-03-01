package com.example.mcpserver.client.mock;

import com.example.mcpserver.client.TalentProfileClient;
import com.example.mcpserver.dto.talentprofile.CandidateProfileV2;
import com.example.mcpserver.store.TalentProfileMockStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mock implementation of TalentProfileClient using comprehensive in-memory data store.
 * Contains 5 realistic candidate profiles with diverse backgrounds:
 *
 * - C001: 7-year experienced SRE/DevOps engineer (Kubernetes, Python expert)
 * - C002: Recent CS graduate from MIT (new grad, junior level)
 * - C003: 8-year senior security engineer (OSCP certified, hired status)
 * - C004: 5-year backend engineer (Python/Django, SLA breach test case)
 * - C005: 6-year frontend engineer (React/TypeScript expert, withdrawn status)
 */
@Component
@Profile("!production")
public class MockTalentProfileClient implements TalentProfileClient {

    @Override
    public Optional<CandidateProfileV2> getProfileV2(String candidateId) {
        return TalentProfileMockStore.getProfileV2(candidateId);
    }
}
