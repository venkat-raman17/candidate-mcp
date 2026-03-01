package com.example.mcpserver.dto.talentprofile;

import java.time.LocalDate;

/**
 * Full Cosmos document representing a candidate's talent profile.
 * Contains base profile, assessments, preferences, questionnaires, and PII fields.
 */
public record CandidateProfileV2(
        String candidateId,
        BaseProfile baseProfile,
        AssessmentResults assessments,
        Preferences preferences,
        QuestionnaireResponses questionnaires,

        // PII fields (stripped in Layer 1 transformation)
        String nationalId,
        String ssnLast4,
        String homeAddress,
        String personalEmail,
        String personalPhone,
        LocalDate dateOfBirth,
        String emergencyContact,

        // Cosmos metadata (stripped)
        String _cosmosPartitionKey,
        String _etag
) {
}
