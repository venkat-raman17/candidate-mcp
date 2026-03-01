package com.example.mcpserver.client;

import com.example.mcpserver.dto.talentprofile.CandidateProfileV2;
import java.util.Optional;

/**
 * Client interface for accessing candidate profile data from the TalentProfile system.
 * <p>
 * This interface provides access to comprehensive candidate profiles including:
 * <ul>
 *   <li>Personal and contact information</li>
 *   <li>Education and work history</li>
 *   <li>Skills and competencies</li>
 *   <li>Assessment results and rankings</li>
 *   <li>Preferences (location, compensation, work style)</li>
 *   <li>Application questionnaire responses</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Note:</strong> Raw profile data contains PII (Personally Identifiable Information)
 * and should be transformed using {@code ProfileTransformer} before exposing to AI agents.
 * </p>
 *
 * @see CandidateProfileV2
 * @since 1.0
 */
public interface TalentProfileClient {

    /**
     * Retrieves a candidate's complete profile using the V2 schema.
     * <p>
     * The V2 profile schema includes enhanced assessment tracking, structured
     * preferences, and comprehensive questionnaire responses. This is the
     * current production version of the candidate profile.
     * </p>
     * <p>
     * <strong>Security Warning:</strong> The returned profile contains sensitive PII
     * including SSN, date of birth, personal contact information, and salary expectations.
     * Always transform profiles using {@code ProfileTransformer} before exposing to
     * external systems or AI agents.
     * </p>
     *
     * @param candidateId the unique identifier of the candidate (e.g., "C001")
     * @return an Optional containing the profile if found, or empty if not found
     * @throws IllegalArgumentException if candidateId is null or empty
     */
    Optional<CandidateProfileV2> getProfileV2(String candidateId);
}
